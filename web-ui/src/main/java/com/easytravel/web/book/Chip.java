package com.easytravel.web.book;

/**
 * WHAT:   One choice chip (a cabin or a sort order).
 *
 * @param value    the enum constant's name
 * @param label    the text
 * @param css      classes (selected or not)
 * @param selected whether it is the current choice
 * @author AoceanP
 */
public record Chip(String value, String label, String css, boolean selected)
{
}
