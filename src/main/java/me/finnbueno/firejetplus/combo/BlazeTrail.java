package me.finnbueno.firejetplus.combo;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.util.ComboManager;
import com.projectkorra.projectkorra.firebending.BlazeArc;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.waterbending.plant.PlantRegrowth;
import me.finnbueno.firejetplus.ability.FireJet;
import me.finnbueno.firejetplus.ability.FireSki;
import me.finnbueno.firejetplus.config.ConfigValue;
import me.finnbueno.firejetplus.config.ConfigValueHandler;
import me.finnbueno.firejetplus.util.FireUtil;
import me.finnbueno.firejetplus.util.OverriddenFireAbility;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Finn Bon
 */
public class BlazeTrail extends OverriddenFireAbility implements ComboAbility {

	private static final double ANGLE_INCREMENT = 5;
	@ConfigValue()
	private double speed = .675;
	@ConfigValue()
	private long cooldown = 4500;
	@ConfigValue()
	private int size = 2;
	@ConfigValue()
	private double angle = 65;
	@ConfigValue()
	private boolean enabled = true;
	@ConfigValue()
	private long fireDuration = 4000;

	private Location loc;
	private FireSki ski;

	/**
	 * This constructor is used to generate config values, do not use
	 */
	public BlazeTrail() {
		super(null);
	}

	public BlazeTrail(Player player, FireSki ski) {
		super(player);
		ConfigValueHandler.get().setFields(this);
		this.ski = ski;
		ski.setSpeed(getDayFactor(this.speed));
		bPlayer.addCooldown(this);
		start();
	}

	@Override
	public void progress() {
		if (ski.isRemoved()) {
			remove();
			return;
		}

		if (ski.getFloor() == null) {
			return;
		}

		if (ThreadLocalRandom.current().nextInt(8) == 0) {
			player.getWorld().playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, .5F, .5F);
		}

		Location loc = ski.getFloor().getRelative(BlockFace.UP).getLocation();
		if (this.loc == loc) {
			return;
		}
		this.loc = loc;

		igniteBlocks();

	}

	private void igniteBlocks() {
		Block belowBlock = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
		List<Block> blocksToIgnite = new ArrayList<>();
		blocksToIgnite.add(belowBlock);
		Block[] surroundingBlocks = {
			// left, right, top, bottom
			belowBlock.getLocation().add(.5, 0, 0).getBlock(),
			belowBlock.getLocation().add(0, 0, .5).getBlock(),
			belowBlock.getLocation().add(-.5, 0, 0).getBlock(),
			belowBlock.getLocation().add(0, 0, -.5).getBlock(),

			// corners
			belowBlock.getLocation().add(.5, 0, .5).getBlock(),
			belowBlock.getLocation().add(-.5, 0, .5).getBlock(),
			belowBlock.getLocation().add(-.5, 0, -.5).getBlock(),
			belowBlock.getLocation().add(.5, 0, -.5).getBlock(),
		};
		for (Block block : surroundingBlocks) {
			if (!blocksToIgnite.contains(block)) {
				blocksToIgnite.add(block);
			}
		}

		for (Block block : blocksToIgnite) {
			if (!isFire(block.getType()) && !isAir(block.getType()) && canFireGrief() && (isPlant(block) || isSnow(block))) {
				new PlantRegrowth(this.player, block);
			}

			if (isIgnitable(block)) {
				this.createTempFire(block.getLocation(), 4000);
			}
		}
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
		return "BlazeTrail";
	}

	@Override
	public Location getLocation() {
		return this.loc;
	}

	@Override
	public void load() {
		super.load();
		FireUtil.registerLanguage(this, "With this combo, a firebender can use their FireSki to set the ground on fire. To use this combo, tap shift on " +
			"FireBlast twice. Then, start skiing.", FireUtil.generateComboInstructions(this));
		ConfigValueHandler.get().registerDefaultValues(new BlazeTrail(), null);
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
		FireSki ski = CoreAbility.getAbility(player, FireSki.class);
		if (ski != null) {
			return new BlazeTrail(player, ski);
		}
		return null;
	}

	@Override
	public ArrayList<ComboManager.AbilityInformation> getCombination() {
		return new ArrayList<>(Arrays.asList(
			new ComboManager.AbilityInformation("FireBlast", ClickType.SHIFT_DOWN),
			new ComboManager.AbilityInformation("FireBlast", ClickType.SHIFT_UP),
			new ComboManager.AbilityInformation("FireBlast", ClickType.SHIFT_DOWN),
			new ComboManager.AbilityInformation("FireBlast", ClickType.SHIFT_UP),
			new ComboManager.AbilityInformation("FireJet", ClickType.LEFT_CLICK),
			new ComboManager.AbilityInformation("FireJet", ClickType.SHIFT_DOWN)
		));
	}
}
