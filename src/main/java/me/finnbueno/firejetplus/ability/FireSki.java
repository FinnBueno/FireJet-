package me.finnbueno.firejetplus.ability;

import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import me.finnbueno.firejetplus.config.ConfigValue;
import me.finnbueno.firejetplus.config.ConfigValueHandler;
import me.finnbueno.firejetplus.util.FireUtil;
import me.finnbueno.firejetplus.util.OverriddenFireAbility;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

/**
 * @author Finn Bon
 */
public class FireSki extends OverriddenFireAbility implements AddonAbility {

	private Permission perm;

	private static final List<Function<Player, Location>> HANDS = Arrays.asList(
		p -> GeneralMethods.getRightSide(p.getLocation(), 0.4).add(0, .3, 0),
		p -> GeneralMethods.getLeftSide(p.getLocation(), 0.4).add(0, .3, 0)
	);

	@ConfigValue()
	private long cooldown = 4000;
	@ConfigValue()
	private long duration = 2500;
	@ConfigValue()
	private double speed = .7;
	@ConfigValue()
	private double maxSteeringAngle = 8;
	@ConfigValue()
	private boolean lockDirectionOnSlotSwap = true;
	@ConfigValue()
	private boolean cancelJetOnDamage = true;
	@ConfigValue()
	private boolean enabled = true;

	private Block floor;
	private Vector direction;

	/**
	 * This constructor is used to generate config values, do not use
	 */
	private FireSki() {
		super(null);
	}

	public FireSki(Player player) {
		super(player);

		ConfigValueHandler.get().setFields(this);

		if (!enabled) {
			return;
		}

		if (!bPlayer.canBendIgnoreBinds(this) || !bPlayer.canBendIgnoreCooldowns(CoreAbility.getAbility("FireJet"))) {
			return;
		}

		this.floor = findFloor();
		this.direction = player.getLocation().getDirection().multiply(getDayFactor(this.speed));
		this.flightHandler.createInstance(player, this.getName());

		start();
	}

	public Block findFloor() {
		for (int i = 1; i < 7; i++) {
			Block below = player.getLocation().getBlock().getRelative(BlockFace.DOWN, i);
			if (GeneralMethods.isSolid(below)) {
				return below;
			}
		}
		return null;
	}

	@Override
	public void progress() {
		if (!bPlayer.canBendIgnoreBindsCooldowns(this) || !player.isSneaking()) {
			remove();
			return;
		}
		if (duration != 0 && getStartTime() + getDayFactor(duration) < System.currentTimeMillis()) {
			remove();
			return;
		}
		if (this.player.getVelocity().length() < speed * .3) {
			remove();
			return;
		}

		this.floor = findFloor();

		player.setFallDistance(0);
		handleVelocity();
		handleAnimation();

		if (!lockDirectionOnSlotSwap || bPlayer.getBoundAbilityName().equals("FireJet")) {
			setDirection();
		}

		if (ThreadLocalRandom.current().nextInt(4) == 0) {
			playFirebendingSound(this.player.getLocation());
		}
	}

	private void handleVelocity() {
		double distanceToGround = this.floor == null ? Double.MAX_VALUE : this.player.getLocation().getY() - this.floor.getY();
		if (distanceToGround > 2.75) {
			this.direction.setY(-0.25D);
		} else if (distanceToGround < 2) {
			this.direction.setY(0.25D);
		} else {
			this.direction.setY(0);
		}

		if (this.floor != null) {
			Block nextFloor = this.floor.getLocation().add(this.direction.clone().setY(0).multiply(1.2)).getBlock();
			if (!GeneralMethods.isSolid(nextFloor)) {
				this.direction.add(new Vector(0.0D, -0.1D, 0.0D));
			} else if (GeneralMethods.isSolid(nextFloor.getRelative(BlockFace.UP))) {
				this.direction.add(new Vector(0.0D, 0.7D, 0.0D));
			}
		}

		this.player.setVelocity(direction);
	}

	private void handleAnimation() {
		Particle particle = bPlayer.hasSubElement(Element.SubElement.BLUE_FIRE) ? Particle.SOUL_FIRE_FLAME : Particle.FLAME;
		int amount = 15;
		double offset = .4;
		for (Function<Player, Location> getHand : HANDS) {
			for (int i = 0; i < amount; i++) {
				Location hand = getHand.apply(player);
				Location loc = hand.add((Math.random() - .5) * offset, (Math.random() - .5) * offset, (Math.random() - .5) * offset);
				double yaw = Math.toRadians(player.getLocation().getYaw());
				Vector particleDirection = new Vector(-Math.sin(yaw), .5, Math.cos(yaw)).multiply(-.6);
				particleDirection = FireUtil.randomizeVector(particleDirection, 50);
				this.player.getWorld()
					.spawnParticle(particle, loc, 0, particleDirection.getX(), particleDirection.getY(), particleDirection.getZ());
			}
		}
	}

	private void setDirection() {
		double yaw = Math.toRadians(player.getLocation().getYaw());
		Vector player = new Vector(-Math.sin(yaw), 0, Math.cos(yaw)).multiply(this.speed);
		Vector initial = this.direction.normalize().clone();
		if (Math.toDegrees(player.angle(initial)) > maxSteeringAngle) {
			Vector cross = player.getCrossProduct(initial);
			this.direction = GeneralMethods.rotateVectorAroundVector(cross, initial, -maxSteeringAngle);
		} else {
			this.direction = player;
		}
		this.direction.normalize().multiply(getDayFactor(this.speed));
	}

	public void handleDamage() {
		if (cancelJetOnDamage) {
			remove();
		}
	}

	@Override
	public void remove() {
		super.remove();
		this.flightHandler.removeInstance(player, getName());
		bPlayer.addCooldown(this);
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public Vector getDirection() {
		return this.direction.clone();
	}

	public Block getFloor() {
		return this.floor;
	}

	@Override
	public boolean isSneakAbility() {
		return true;
	}

	@Override
	public boolean isHarmlessAbility() {
		return false;
	}

	@Override
	public boolean isHiddenAbility() {
		return true;
	}

	@Override
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public String getName() {
		return "FireSki";
	}

	@Override
	public Location getLocation() {
		return player.getLocation();
	}

	@Override
	public void load() {
		super.load();
		ConfigValueHandler.get().registerDefaultValues(new FireSki(), "FireJet");
	}

	@Override
	public void stop() {
		ConfigValueHandler.get().unregister(this);
	}

	@Override
	public String getAuthor() {
		return FireJet.AUTHOR;
	}

	@Override
	public String getVersion() {
		return FireJet.VERSION;
	}
}
