package dev.belikhun.lunamanhunt.util;

import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class ItemStackCodec {
	private ItemStackCodec() {
	}

	public static List<String> encodeList(List<ItemStack> items) {
		List<String> encoded = new ArrayList<>();
		if (items == null) {
			return encoded;
		}
		for (ItemStack item : items) {
			String value = encodeItem(item);
			if (value != null) {
				encoded.add(value);
			}
		}
		return encoded;
	}

	public static List<ItemStack> decodeList(List<String> encoded) {
		List<ItemStack> items = new ArrayList<>();
		if (encoded == null) {
			return items;
		}
		for (String value : encoded) {
			ItemStack item = decodeItem(value);
			if (item != null) {
				items.add(item);
			}
		}
		return items;
	}

	private static String encodeItem(ItemStack item) {
		if (item == null) {
			return null;
		}
		try {
			byte[] data = item.serializeAsBytes();
			return Base64.getEncoder().encodeToString(data);
		} catch (Exception ignored) {
			return null;
		}
	}

	private static ItemStack decodeItem(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			byte[] data = Base64.getDecoder().decode(value);
			return ItemStack.deserializeBytes(data);
		} catch (Exception ignored) {
			return null;
		}
	}
}
