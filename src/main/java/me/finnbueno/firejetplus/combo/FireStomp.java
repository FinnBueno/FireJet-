package me.finnbueno.firejetplus.combo;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.util.ComboManager;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.DamageHandler;
import me.finnbueno.firejetplus.ability.FireJet;
import me.finnbueno.firejetplus.config.ConfigValue;
import me.finnbueno.firejetplus.config.ConfigValueHandler;
import me.finnbueno.firejetplus.util.FireUtil;
import me.finnbueno.firejetplus.util.OverriddenFireAbility;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Finn Bon
 */
public class FireStomp extends OverriddenFireAbility implements ComboAbility, AddonAbility {

	@ConfigValue()
	private long cooldown = 2500;
	@ConfigValue()
	private int radius = 2;
	@ConfigValue()
	private double activationDuration = 1500;
	@ConfigValue()
	private double damage = 3;
	@ConfigValue()
	private double knockback = .5;
	@ConfigValue()
	private double fallDamageReduction = 12;
	@ConfigValue()
	private boolean enabled = true;

	private double yVelocity, fallDistance;

	/**
	 * This constructor is used to generate config values, do not use
	 */
	private FireStomp() {
		super(null);
	}

	public FireStomp(Player player) {
		super(player);
		ConfigValueHandler.get().setFields(this);
		this.yVelocity = player.getVelocity().getY();
		this.fallDistance = player.getFallDistance();
		start();
	}

	@Override
	public void progress() {
		if (!bPlayer.canBendIgnoreBindsCooldowns(this)) {
			remove();
			return;
		}
		// not hit the ground during the activation period
		if (getStartTime() + activationDuration < System.currentTimeMillis()) {
			remove();
			return;
		}
		// check if we've hit the ground
		if (isOnGround()) {
			if (yVelocity < -.75 && fallDistance > 4) {
				impact();
			} else {
				remove();
			}
			return;
		}

		yVelocity = this.player.getVelocity().getY();
		fallDistance = this.player.getFallDistance();

		if (ThreadLocalRandom.current().nextInt(4) == 0) {
			playFirebendingSound(player.getEyeLocation());
		}
		playFirebendingParticles(player.getLocation().add(0, .3, 0), 10, .4, .4, .4);
	}

	private void impact() {
		Location center = player.getLocation().add(0, .1, 0);
		int amount = 300;
		double offset = .5;
		Particle fire = FireUtil.getFireParticle(bPlayer);
		for (int i = 0; i < amount; i++) {
			double xOffset = (Math.random() - .5) * offset;
			double zOffset = (Math.random() - .5) * offset;
			Location loc = center.clone().add(xOffset, 0, zOffset);
			Vector outward = GeneralMethods.getDirection(center, loc);
			outward.normalize().multiply(.35 * Math.random() + .15).setY(Math.random() * .15);
			player.getWorld().spawnParticle(fire, loc, 0, outward.getX(), outward.getY(), outward.getZ());
		}

		playFirebendingSound(center);
		player.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1, 2);

		GeneralMethods.getEntitiesAroundPoint(center, radius)
			.stream()
			.filter(e -> e.getUniqueId() != player.getUniqueId())
			.filter(e -> e instanceof LivingEntity)
			.map(e -> (LivingEntity) e)
			.forEach(e -> {
				DamageHandler.damageEntity(e, getDayFactor(damage), this);
				Vector knockback = GeneralMethods.getDirection(center, e.getLocation()).normalize().multiply(getDayFactor(this.knockback));
				if (!GeneralMethods.isSolid(e.getLocation().add(0, -.1, 0).getBlock())) {
					knockback.multiply(.6);
				}
				e.setVelocity(e.getVelocity().add(knockback));
			});

		bPlayer.addCooldown(this);
		remove();
	}

	public void handleFallDamage(EntityDamageEvent event) {
		if (event.getDamage() <= fallDamageReduction) {
			event.setCancelled(true);
		} else {
			event.setDamage(event.getDamage() - fallDamageReduction);
		}
	}

	private boolean isOnGround() {
		return GeneralMethods.isSolid(player.getLocation().subtract(0, .1, 0).getBlock());
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
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public String getName() {
		return "FireStomp";
	}

	@Override
	public Location getLocation() {
		return player.getLocation();
	}

	@Override
	public void load() {
		super.load();
		FireUtil.registerLanguage(this, "This combo allows a firebender to perform a downward kick, striking fire around them when hitting the ground. " +
			"To use, perform the combo. You then have 1.5 seconds to hit the ground at high enough velocity. When you do so, fire will strike on the impact area, knocking " +
			"back and damaging those around you, as well as reducing your fall damage.", FireUtil.generateComboInstructions(this));
		ConfigValueHandler.get().registerDefaultValues(new FireStomp(), null);
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

	@Override
	public Object createNewComboInstance(Player player) {
		BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
		if (bPlayer == null) {
			return null;
		}
		if (!bPlayer.canBendIgnoreBinds(this)) {
			return null;
		}
		return new FireStomp(player);
	}

	@Override
	public ArrayList<ComboManager.AbilityInformation> getCombination() {
		return new ArrayList<>(Arrays.asList(
			new ComboManager.AbilityInformation("Blaze", ClickType.SHIFT_DOWN),
			new ComboManager.AbilityInformation("Blaze", ClickType.LEFT_CLICK),
			new ComboManager.AbilityInformation("Blaze", ClickType.SHIFT_UP)
		));
	}
}
