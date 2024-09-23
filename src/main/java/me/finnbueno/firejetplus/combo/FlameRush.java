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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Finn Bon
 */
public class FlameRush extends OverriddenFireAbility implements ComboAbility, AddonAbility {

	@ConfigValue()
	private long cooldown = 1500;
	@ConfigValue()
	private long duration = 1000;
	@ConfigValue()
	private double speed = 1.7;
	@ConfigValue()
	private double knockback = .7;
	@ConfigValue()
	private double maxSteeringAngle = 2.5;
	@ConfigValue()
	private double damage = 2;
	@ConfigValue()
	private boolean enabled = true;

	private FireJet jet;

	/**
	 * This constructor is used to generate config values, do not use
	 */
	private FlameRush() {
		super(null);
	}

	public FlameRush(Player player, FireJet jet) {
		super(player);
		this.jet = jet;
		if (this.jet.getChargeFactor() < .6) {
			return;
		}
		ConfigValueHandler.get().setFields(this);
		if (this.duration != 0) {
			this.jet.setDuration(this.duration);
		}
		this.jet.setSpeed(getDayFactor(this.speed));
		this.jet.setMaxSteeringAngle(this.maxSteeringAngle);
		this.player.getWorld().playSound(player.getEyeLocation(), Sound.ENTITY_GENERIC_EXPLODE, .7f, .7f);
		start();
	}

	@Override
	public void progress() {
		if (!bPlayer.canBendIgnoreBindsCooldowns(this)) {
			remove();
			return;
		}

		if (jet.isRemoved()) {
			remove();
			return;
		}

		this.player.getWorld().spawnParticle(Particle.SMOKE_LARGE, player.getLocation(), 10, .2, .2, .2, 0.05);

		if (ThreadLocalRandom.current().nextInt(4) == 0) {
			this.player.getWorld().playSound(player.getEyeLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1f, .4f);
		}


		GeneralMethods.getEntitiesAroundPoint(getLocation(), 1).stream()
			.filter(e -> e.getUniqueId() != player.getUniqueId())
			.filter(e -> e instanceof LivingEntity)
			.map(e -> (LivingEntity) e)
			.forEach(e -> {
				e.setVelocity(
					this.jet.getDirection().normalize().add(
						GeneralMethods.getDirection(
							player.getLocation(),
							e.getLocation()
						).normalize().multiply(.2)
					).normalize().multiply(getDayFactor(this.knockback))
				);
				DamageHandler.damageEntity(e, getDayFactor(this.damage), this);
			});
	}

	@Override
	public void remove() {
		super.remove();
		bPlayer.addCooldown(this);
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
		return "FlameRush";
	}

	@Override
	public Location getLocation() {
		return player.getLocation();
	}

	@Override
	public void load() {
		super.load();
		FireUtil.registerLanguage(this, "With this combo, a firebender can greatly accelerate their FireJet, at the cost of steering capacity. To " +
			"use, the FireJet charge bar must be charged to red. When activated, your FireJet will become much faster and knock aside anyone you hit. However, steering " +
			"becomes nearly impossible.", "Instructions: Blaze (Shift Down) > FireBlast (Shift Up) > Charge FireJet to red");
		ConfigValueHandler.get().registerDefaultValues(new FlameRush(), null);
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
		FireJet jet = getAbility(player, FireJet.class);
		if (jet != null) {
			long timeSinceStart = System.currentTimeMillis() - jet.getStartTime();
			if (timeSinceStart + 50 > jet.getMaxChargeTime()) {
				return null;
			}
			jet.onFlyStart(() -> new FlameRush(player, jet));
		}
		return null;
	}

	@Override
	public ArrayList<ComboManager.AbilityInformation> getCombination() {
		return new ArrayList<>(Arrays.asList(
			new ComboManager.AbilityInformation("Blaze", ClickType.SHIFT_DOWN),
			new ComboManager.AbilityInformation("FireBlast", ClickType.SHIFT_UP),
			new ComboManager.AbilityInformation("FireJet", ClickType.SHIFT_DOWN),
			new ComboManager.AbilityInformation("FireJet", ClickType.SHIFT_UP)
		));
	}
}
