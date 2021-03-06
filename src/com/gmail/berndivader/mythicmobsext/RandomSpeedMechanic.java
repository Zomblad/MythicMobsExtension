package com.gmail.berndivader.mythicmobsext;

import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang3.Range;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

import io.lumine.xikage.mythicmobs.adapters.AbstractEntity;
import io.lumine.xikage.mythicmobs.io.MythicLineConfig;
import io.lumine.xikage.mythicmobs.skills.INoTargetSkill;
import io.lumine.xikage.mythicmobs.skills.ITargetedEntitySkill;
import io.lumine.xikage.mythicmobs.skills.SkillMechanic;
import io.lumine.xikage.mythicmobs.skills.SkillMetadata;

public class RandomSpeedMechanic 
extends 
SkillMechanic
implements
ITargetedEntitySkill,
INoTargetSkill {
	private Range<Double> r1;
	public static String ss1="0.3to0.45";

	public RandomSpeedMechanic(String skill, MythicLineConfig mlc) {
		super(skill, mlc);
		String s1=mlc.getString("range",ss1).toUpperCase();
		if (!s1.contains("TO")) s1=ss1;
		String[]arr1=s1.split("TO");
		r1=Range.between(Double.parseDouble(arr1[0]),Double.parseDouble(arr1[1]));
	}

	@Override
	public boolean cast(SkillMetadata data) {
		return d(data.getCaster().getEntity());
	}

	@Override
	public boolean castAtEntity(SkillMetadata data, AbstractEntity target) {
		return d(target);
	}
	
	private boolean d(AbstractEntity target) {
		if (target.isLiving()) {
			LivingEntity le1=(LivingEntity)target.getBukkitEntity();
			double d1=ThreadLocalRandom.current().nextDouble(r1.getMinimum(),r1.getMaximum());
            le1.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(d1);
    		return true;
		}
		return false;
	}
	

}
