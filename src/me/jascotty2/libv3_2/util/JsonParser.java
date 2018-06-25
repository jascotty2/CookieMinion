/**
 * Copyright (C) 2018 Jacob Scott <jascottytechie@gmail.com> Description:
 * Provides methods for parsing and encoding json
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package me.jascotty2.libv3_2.util;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class JsonParser {

	// from -2147483648 up to 2147483647
	static final String INT_PATTERN = "[+-]?((1?[0-9]{1,9})"
			+ "|(20[0-9]{8})"
			+ "|(213[0-9]{7})"
			+ "|(2146[0-9]{6})"
			+ "|(21473[0-9]{5})"
			+ "|(214747[0-9]{4})"
			+ "|(2147482[0-9]{3})"
			+ "|(21474835[0-9]{2})"
			+ "|(214748363[0-9]))"
			+ "|(214748364[0-7])|-(214748364[0-8])";

	public static Map<String, Object> parseJSON(String data) throws ParseException {
		return parseJSON(data, false);
	}

	public static Map<String, Object> parseJSON(String data, final boolean unescapeChars) throws ParseException {
		if (data == null) {
			throw new IllegalArgumentException("data cannot be null");
		}
		data = data.trim();
		if (!data.isEmpty() && data.charAt(0) == '{') {
			// find ending '}'
			int end = getMapStrEnd(data, 1);
			if (end < 0) {
				throw new ParseException("Cannot find ending '}'", 0);
			}
			data = data.substring(1, end);

			return _parseJSONmap(null, data, unescapeChars);
		} else if (!data.isEmpty() && data.charAt(0) == '[') {
			// find ending ']'
			int end = getListStrEnd(data, 1);
			if (end < 0) {
				throw new ParseException("Cannot find ending ']'", 0);
			}
			final String dat = data.substring(1, end);

			return new HashMap<String, Object>() {
				{
					put("", _parseJSONlist(null, dat, unescapeChars));
				}
			};
		}
		return null;
	}

	@SuppressWarnings({"empty-statement", "unchecked"})
	protected static Map<String, Object> _parseJSONmap(String currentKey, String data, boolean unescapeChars) throws ParseException {
		//System.out.println("parsing map " + currentKey + ": " + data);
		Map<String, Object> wr = new HashMap<String, Object>();
		for (int i = 0; i < data.length(); ++i) {
			for (; i < data.length() && Character.isWhitespace(data.charAt(i)); ++i);
			if (i >= data.length()) {
				break;
			}
//			if (data.charAt(i) != '"') {
//				throw new ParseException("Expecting key quote, none found" + (currentKey != null ? " at " + currentKey : "") + "\n"
//					+ (i < 5 ? (data.length() < 30 ? data : data.substring(0, 30)) : (data.length() - i < 30 ? data.substring(i - 5) : data.substring(i - 5, i + 30))), i);
//			}
			int end;
			if (data.charAt(i) == '"') {
				// key is enclosed in double quotes
				end = data.indexOf("\"", i + 1);
				// check to see if quote is escaped by a non-escaped \
				while (end > 0 && escapedCharacter(data, end)) {
					end = data.indexOf("\"", end + 1);
				}
			} else {
				// assume we're parsing a lenient string
				end = data.indexOf(":", i + 1);
				--i;
			}
			if (end < 0) {
				throw new ParseException("Cannot find ending key quote \n  Error at " + (currentKey != null ? currentKey : "") + " \""
						+ (i < 5 ? (data.length() < 30 ? data : data.substring(0, 30)) : (data.length() - i < 30 ? data.substring(i - 5) : data.substring(i - 5, i + 30))), i);
			}
			String key = data.substring(i + 1, end).replace("\\\"", "\"").replace("\\/", "/").replace("\\\\", "\\");
			if (unescapeChars) {
				key = convertUnicode(key.replace("\\n", "\n").replace("\\t", "\t").replace("\\r", "\r"));
			}

			i = data.indexOf(":", end) + 1;
			for (; i < data.length() && Character.isWhitespace(data.charAt(i)); ++i);
			switch (data.charAt(i)) {
				case '"':
					// string value
					//end = data.indexOf("\"", i + 1);
					//while (end > 0 && escapedCharacter(data, end)) {
					//	end = data.indexOf("\"", end + 1);
					//}
					end = getStrEnd(data, i + 1);
					if (end < 0) {
						throw new ParseException("Cannot find ending value quote \n  Error at " + (currentKey != null ? currentKey : "") + " \""
								+ (i < 5 ? (data.length() < 30 ? data : data.substring(0, 30)) : (data.length() - i < 30 ? data.substring(i - 5) : data.substring(i - 5, i + 30))), i);
					}
					if (unescapeChars) {
						wr.put(key, convertUnicode(data.substring(i + 1, end).replace("\\\"", "\"").replace("\\/", "/").replace("\\\\", "\\")
								.replace("\\n", "\n").replace("\\t", "\t").replace("\\r", "\r")));
					} else {
						wr.put(key, data.substring(i + 1, end).replace("\\\"", "\"").replace("\\/", "/").replace("\\\\", "\\"));
					}
					i = end;
					break;
				case '[':
					end = getListStrEnd(data, i + 1);
					if (end < 0) {
						throw new ParseException("Cannot find ending ']'" + (currentKey != null ? " for " + currentKey : ""), 0);
					}
					if ((end) <= (i + 1)) {
						wr.put(key, new ArrayList());
					} else {
						wr.put(key, _parseJSONlist(currentKey == null ? key : currentKey + "." + key, data.substring(i + 1, end), unescapeChars));
					}
					i = end + 1;
					break;
				case '{':
					end = getMapStrEnd(data, i + 1);
					if (end < 0) {
						throw new ParseException("Cannot find ending '}'" + (currentKey != null ? " for " + currentKey : ""), 0);
					}
					if ((end) <= (i + 1)) {
						wr.put(key, new HashMap<String, Object>());
					} else {
						wr.put(key, _parseJSONmap(currentKey == null ? key : currentKey + "." + key, data.substring(i + 1, end), unescapeChars));
					}
					i = end + 1;
					break;
				default:
					// integer or boolean
					if (i + 4 <= data.length() && data.substring(i, i + 4).equalsIgnoreCase("true")) {
						wr.put(key, true);
						i += 4;
					} else if (i + 4 <= data.length() && data.substring(i, i + 4).equalsIgnoreCase("null")) {
						wr.put(key, null);
						i += 4;
					} else if (i + 5 <= data.length() && data.substring(i, i + 5).equalsIgnoreCase("false")) {
						wr.put(key, false);
						i += 5;
					} else {
						// number
						for (end = i; end < data.length() && (Character.isDigit(data.charAt(end)) || data.charAt(end) == '.' || data.charAt(end) == '-'); ++end);
						final String val = data.substring(i, end);
						if (val.isEmpty()) {
							wr.put(key, null);
						} else if (val.contains(".")) {
							wr.put(key, Double.parseDouble(val));
						} else if (Pattern.matches(INT_PATTERN, val)) {
							wr.put(key, Integer.parseInt(val));
						} else if (val.isEmpty()) {
							System.out.println("empty key: " + key);
							System.out.println(data.substring(i - 50 > 0 ? i - 50 : 0, end + 50 > data.length() ? data.length() : end + 50));
						} else {
							wr.put(key, Long.parseLong(val));
						}
						i = end;
					}
					break;
			}

			if ((end = data.indexOf(',', i)) > 0) {
				i = end;
			}
		}
		return wr;
	}

	protected static boolean escapedCharacter(String data, int position) {
		boolean esc = false;
		for (int j = position - 1; j >= 0; --j) {
			if (data.charAt(j) == '\\') {
				esc = !esc;
			} else {
				break;
			}
		}
		return esc;
	}

	@SuppressWarnings({"empty-statement", "unchecked"})
	protected static List<Object> _parseJSONlist(String currentKey, String data, boolean unescapeChars) throws ParseException {
		List<Object> ls = new ArrayList<Object>();
		int n = 0;
		int end;
		for (int i = 0; i < data.length(); ++i) {
			for (; i < data.length() && Character.isWhitespace(data.charAt(i)); ++i);
			if (i >= data.length()) {
				break;
			}
			switch (data.charAt(i)) {
				case '"':
					// string value
					end = getStrEnd(data, i + 1);
					if (end < 0) {
						throw new ParseException("Cannot find ending value quote \n  Error at " + (currentKey != null ? currentKey + "[" + n + "]" : "[" + n + "]") + " \""
								+ (i < 5 ? (data.length() < 30 ? data : data.substring(0, 30)) : (data.length() - i < 30 ? data.substring(i - 5) : data.substring(i - 5, i + 30))), i);
					}
					if (unescapeChars) {
						ls.add(convertUnicode(data.substring(i + 1, end).replace("\\\"", "\"").replace("\\/", "/").replace("\\\\", "\\")
								.replace("\\n", "\n").replace("\\t", "\t").replace("\\r", "\r")));
					} else {
						ls.add(data.substring(i + 1, end).replace("\\\"", "\"").replace("\\/", "/").replace("\\\\", "\\"));
					}
					i = end;
					break;
				case '[':
					end = getListStrEnd(data, i + 1);
					if (end < 0) {
						throw new ParseException("Cannot find ending ']' for " + (currentKey != null ? currentKey + "[" + n + "]" : "[" + n + "]"), 0);
					}
					ls.add(_parseJSONlist(currentKey == null ? "[" + n + "]" : currentKey + "[" + n + "]", data.substring(i + 1, end), unescapeChars));
					i = end + 1;
					break;
				case '{':
					end = getMapStrEnd(data, i + 1);
					if (end < 0) {
						throw new ParseException("Cannot find ending '}' for " + (currentKey != null ? currentKey + "[" + n + "]" : "[" + n + "]"), 0);
					}
					if ((end) <= (i + 1)) {
						ls.add(new HashMap<String, Object>());
					} else {
						ls.add(_parseJSONmap(currentKey == null ? "[" + n + "]" : currentKey + "[" + n + "]", data.substring(i + 1, end), unescapeChars));
					}
					i = end + 1;
					break;
				default:
					// integer or boolean
					if (i + 4 < data.length() && data.substring(i, i + 4).equalsIgnoreCase("true")) {
						ls.add(true);
						i += 4;
					} else if (i + 4 <= data.length() && data.substring(i, i + 4).equalsIgnoreCase("null")) {
						ls.add(null);
						i += 4;
					} else if (i + 5 < data.length() && data.substring(i, i + 5).equalsIgnoreCase("false")) {
						ls.add(false);
						i += 5;
					} else {
						// number
						end = i;
						for (; end < data.length() && (Character.isDigit(data.charAt(end)) || data.charAt(end) == '.'); ++end);
						final String val = data.substring(i, end);
						if (val.isEmpty()) {
							ls.add(null);
						} else if (val.contains(".")) {
							ls.add(Double.parseDouble(val));
						} else if (Pattern.matches(INT_PATTERN, val)) {
							ls.add(Integer.parseInt(val));
						} else {
							ls.add(Long.parseLong(val));
						}
						i = end;
					}
					break;
			}
			++n;
			if ((i = data.indexOf(',', i)) < 0) {
				break;
			}
		}
		return ls;
	}

	public static String encodeJSON(Object data) {
		if (data == null) {
			throw new IllegalArgumentException("data cannot be null");
		}
		if (data instanceof Map) {
			return _encodeJSONmap((Map) data);
		} else if (data instanceof List) {
			return _encodeJSONlist((List) data);
		} else if (data instanceof Number) {
			return ((Number) data).toString();
		} else {
			//use as a string
			return String.format("\"%s\"", data.toString()
					.replace("\\", "\\\\").replace("\"", "\\\"")//.replace("/", "\\/")
					.replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r"));
		}
	}

	protected static String _encodeJSONmap(Map<String, Object> data) {
		StringBuilder map = new StringBuilder("{");
		int left = data.size();
		for (Map.Entry<String, Object> e : data.entrySet()) {
			map.append("\"").append(e.getKey()).append("\": ");
			if (e.getValue() instanceof List) {
				map.append(_encodeJSONlist((List) e.getValue()));
			} else if (e.getValue() instanceof Map) {
				map.append(_encodeJSONmap((Map) e.getValue()));
			} else if (e.getValue() instanceof Double) {
				map.append(printDecimal((Double) e.getValue()));
			} else if (e.getValue() instanceof String) {
				map.append("\"").append(encodeUnicode(((String) e.getValue())
						.replace("\\", "\\\\").replace("\"", "\\\"")//.replace("/", "\\/")
						.replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r"))).append("\"");
			} else {
				map.append(e.getValue());
			}
			if (--left > 0) {
				map.append(",");
			}
		}
		return map.append("}").toString();
	}

	protected static String _encodeJSONlist(List<Object> data) {
		StringBuilder map = new StringBuilder("[");
		for (int i = 0; i < data.size(); ++i) {
			Object o = data.get(i);
			if (o instanceof List) {
				map.append(_encodeJSONlist((List) o));
			} else if (o instanceof Map) {
				map.append(_encodeJSONmap((Map) o));
			} else if (o instanceof Double) {
				map.append(printDecimal((Double) o));
			} else if (o instanceof String) {
				map.append("\"").append(encodeUnicode(((String) o)
						.replace("\\", "\\\\").replace("\"", "\\\"")//.replace("/", "\\/")
						.replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r"))).append("\"");
			} else {
				map.append(o);
			}
			if (i + 1 < data.size()) {
				map.append(",");
			}
		}
		return map.append("]").toString();
	}

	@SuppressWarnings("unchecked")
	public static void printMap(Map<String, Object> dat, int depth) {
		if (dat == null) {
			System.out.println("null");
			return;
		}
		for (Map.Entry<String, Object> e : dat.entrySet()) {
			for (int i = 0; i < depth; ++i) {
				System.out.print("  ");
			}
			System.out.print("-" + e.getKey() + ": ");
			if (e.getValue() instanceof List) {
				System.out.println();
				printList((List<Object>) e.getValue(), depth + 1);
				//System.out.println("[" + Str.concatStr((List) e.getValue(), ", ") + "]");
			} else if (e.getValue() instanceof Map) {
				System.out.println();
				printMap((Map<String, Object>) e.getValue(), depth + 1);
			} else if (e.getValue() instanceof String) {
				System.out.println("\"" + e.getValue() + "\"");
			} else if (e.getValue() instanceof Double) {
				System.out.println(printDecimal((Double) e.getValue()));
			} else {
				System.out.println(e.getValue());
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static void printList(List<Object> dat, int depth) {
		for (Object o : dat) {
			for (int i = 0; i < depth; ++i) {
				System.out.print("  ");
			}
			System.out.print(":");
			if (o instanceof List) {
				System.out.println();
				printList((List<Object>) o, depth + 1);
			} else if (o instanceof Map) {
				System.out.println();
				printMap((Map<String, Object>) o, depth + 1);
			} else if (o instanceof String) {
				System.out.println("\"" + o + "\"");
			} else if (o instanceof Double) {
				System.out.println(printDecimal((Double) o));
			} else {
				System.out.println(o);
			}
		}
	}

	/**
	 * returns parsed list portion of a XMLHttpRequest each element of the array
	 * is the whole
	 *
	 * @param key what the key for the list is
	 * @param data the data to parse
	 * @return
	 */
	public static List<Object> getJsonItemLists(String key, String data) {
		// [] denotes list items
		// {} denotes key -> value lists

		ArrayList<Object> lists = null;
		key = "\"" + key + "\":";
		int i = data.indexOf(key);
		if (i != -1) {
			try {
				lists = new ArrayList<Object>();
				i += key.length();
				while (i < data.length() && data.charAt(i - 1) != '[') {
					++i;
				}
				while (i < data.length() && Character.isWhitespace(data.charAt(i))) {
					++i;
				}

				boolean listOnly = data.charAt(i) != '{';

				String dat = data.substring(i, getListStrEnd(data, i));
				if (listOnly) {
					i = 0;
					ArrayList<String> wr = new ArrayList<String>();
					while (i + 1 < dat.length() && dat.charAt(i) != ']') {
						// all values seperated by commas
						int e = dat.indexOf(",", i);
						if (dat.charAt(i) == '"') {
							wr.add(e > 0 ? dat.substring(i + 1, e - 2) : dat.substring(i + 1, dat.length() - i - 1));
						} else {
							wr.add(null);
						}
						if (e > 0) {
							i = e + 1;
						} else {
							i = dat.length();
						}
					}
					lists.add(wr);
				} else {
					i = 1;
					while (i > 0 && i + 1 < dat.length()) {
						Map<String, Object> wr = new HashMap<String, Object>();
						while (i + 1 < dat.length() && dat.charAt(i) != '}') {
							// all keys begin with a quote
							int start = i = getAssertedIndex(dat, "\"", i) + 1;
							// now find the matching end quote
							int end = getAssertedIndex(dat, "\"", i + 1);
							while (dat.charAt(end - 1) == '\\') {
								end = getAssertedIndex(dat, "\"", end + 1);
							}
							String k = dat.substring(i, end);
							Object toAdd = null;
							i = getAssertedIndex(dat, ":", i) + 1;
							while (Character.isWhitespace(data.charAt(i))) {
								++i;
							}
							if (dat.charAt(i) == '"') {
								++i;
								end = getAssertedIndex(dat, "\"", i);
								while (dat.charAt(end - 1) == '\\') {
									end = getAssertedIndex(dat, "\"", end + 1);
								}
								toAdd = dat.substring(i, end);
								++end;
							} else {
								if (dat.charAt(i) == '[') {
									toAdd = getJsonItemLists(k, dat.substring(start - 1));
									end = getListStrEnd(dat, i + 1);
								} else {
									end = i;
									for (; end < dat.length(); ++end) {
										char c = dat.charAt(end);
										if (c == ',' || c == ':' || c == '[' || c == ']' || c == '}') {
											break;
										}
									}
									String v = dat.substring(i, end);
									if (!v.toLowerCase().equals("null")) {
										if (v.equalsIgnoreCase("false")) {
											toAdd = false;
										} else if (v.equalsIgnoreCase("true")) {
											toAdd = true;
										} else {
											try {
												if (v.contains(".")) {
													toAdd = Double.parseDouble(v);
												} else {
													toAdd = Integer.parseInt(v);
												}
											} catch (Exception e) {
												toAdd = null;
											}
										}
									}
								}
							}
							wr.put(k, toAdd);
							i = end;
						}
						lists.add(wr);
						i = dat.indexOf('{', i);
					}
				}
			} catch (Exception ex) {
				Logger.getLogger(JsonParser.class.getName()).log(Level.SEVERE, null, ex);
				//System.out.println(Str.getStackStr(ex));
			}
		}
		return lists;
	}

	public static String getItemListStr(String key, String data) {
		key = "\"" + key + "\":[";
		int i = data.indexOf(key);
		if (i != -1) {
			i += key.length();
			return data.substring(i, getListStrEnd(data, i));
		}
		return null;
	}

	protected static int getStrEnd(String data, int start) {
		for (int i = start; i < data.length(); ++i) {
			if (data.charAt(i) == '"') {
				// double-check if this is an escaped character
				boolean esc = false;
				if (data.charAt(i - 1) == '\\') {
					esc = true;
					// check \ escape
					for (int j = i - 2; j > start; --j) {
						if (data.charAt(j) == '\\') {
							esc = !esc;
						} else {
							break;
						}
					}
				}
				if (!esc) {
					return i;
				}
			}
		}
		return -1;
	}

	protected static int getListStrEnd(String data, int start) {
		int depth = 1;
		boolean inStr = false;
		for (int i = start; i < data.length(); ++i) {
			if (data.charAt(i) == '"') {
				if (inStr) {
					// double-check if this is an escaped character
					if (data.charAt(i - 1) != '\\') {
						inStr = false;
					} else {
						// check \ escape
						boolean esc = false;
						for (int j = i - 2; j > start; --j) {
							if (data.charAt(j) == '\\') {
								esc = !esc;
							} else {
								break;
							}
						}
						if (esc) {
							// the \ is escaped - quote is valid
							inStr = false;
						}
					}
				} else {
					// now in a str
					inStr = true;
				}
			}
			if (!inStr) {
				if (data.charAt(i) == ']') {
					if (--depth <= 0) {
						return i;
					}
				} else if (data.charAt(i) == '[') {
					++depth;
				}
			}
		}
		return -1;
	}

	protected static int getMapStrEnd(String data, int start) {
		int depth = 1;
		boolean inStr = false;
		for (int i = start; i < data.length(); ++i) {
			if (data.charAt(i) == '"') {
				if (inStr) {
					// double-check if this is an escaped character
					// note: if the \ is escaped, this gets tricky
					if (data.charAt(i - 1) != '\\') {
						inStr = false;
					} else {
						// check \ escape
						boolean esc = false;
						for (int j = i - 2; j > start; --j) {
							if (data.charAt(j) == '\\') {
								esc = !esc;
							} else {
								break;
							}
						}
						if (esc) {
							// the \ is escaped - quote is valid
							inStr = false;
						}
					}
				} else {
					// now in a str
					inStr = true;
				}
			}
			if (!inStr) {
				if (data.charAt(i) == '}') {
					if (--depth <= 0) {
						return i;
					}
				} else if (data.charAt(i) == '{') {
					++depth;
				}
			}
		}
		return -1;
	}

	public static String getData(String itemList, String tag) {
		if (itemList != null) {
			tag = "\"" + tag + "\":";
			int i = itemList.indexOf(tag);
			if (i != -1) {
				i += tag.length();
				// if starts with a quote, inc by 1 and go to end quote
				// else, continue to the ending comma
//				int end = (requestData.charAt(i) == '"')
//						? requestData.indexOf("\"", ++i)
//						: requestData.indexOf(",", i);
				int end;
				if (itemList.charAt(i) == '"') {
					end = itemList.indexOf("\"", ++i);
				} else {
					int a = itemList.indexOf("}", i);
					int b = itemList.indexOf("]", i);
					int c = itemList.indexOf(",", i);
					// choose the first control char to break at
					end = (a != -1 && a < b && a < c)
							? a : ((b != -1 && b < a && b < c) ? b : c);
				}
				if (end != -1) {
					return itemList.substring(i, end);
				} else {
					return itemList.substring(i);
				}
			}
		}
		return null;
	}

	public static String getHTMLDataWithoutTags(String data) {
		data = data.replace("&nbsp;", " ").replace("&amp;", "&");
		if (data.contains("<")) {
			StringBuilder dat = new StringBuilder();
			boolean in_tag = false;
			for (char c : data.toCharArray()) {
				if (c == '<') {
					in_tag = true;
				} else if (c == '>') {
					in_tag = false;
				} else if (!in_tag) {
					dat.append(c);
				}
			}
			return dat.toString();
		}
		return data;
	}

	private static int getAssertedIndex(String str, String search, int start) throws Exception {
		int i = str.indexOf(search, start);
		if (i == -1) {
			throw new Exception(String.format("\"%s\" not found in string", search));
		}
		return i;
	}

	public static String convertUnicode(String str) {
		if (str.contains("\\u")) {
			StringBuilder d = new StringBuilder();
			int i, last = 0;
			while ((i = str.indexOf("\\u", last)) >= 0) {
				if (i + 6 > str.length()) {
					d.append(str.substring(last));
					last = i + 6;
				} else {
					d.append(str.substring(last, i));
					i += 2; // skip the 'u'
					// looking for 4 hex chars
					String code = str.substring(i, i + 4).toUpperCase();
					if (code.matches("[0-9A-F]{4}")) {
						// all good!
						d.append((char) Integer.parseInt(Numbers.changeBase(code, 16, 10)));
						last = i + 4;
					} else {
						d.append("\\u");
						last = i;
					}
				}
			}
			if (last < str.length()) {
				d.append(str.substring(last));
			}
			return d.toString();
		}
		return str;
	}

	public static String encodeUnicode(String str) {
		if (str.matches(".*[^\\x00-\\x7F].*")) {
			StringBuilder d = new StringBuilder();
			final int last = str.length();
			for (int i = 0; i < last; ++i) {
				final char c = str.charAt(i);
				if ((int) c > 127) {

					d.append("\\u");

					final String val = Numbers.changeBase(String.valueOf((int) c), 10, 16);

					for (int n = 4 - val.length(); n >= 0; --n) {
						d.append('0');
					}
					d.append(val);

				} else {
					d.append(c);
				}
			}
			return d.toString();
		}
		return str;
	}

	static String printDecimal(float f) {
		return printDecimal((double) f);
	}

	@SuppressWarnings("empty-statement")
	static String printDecimal(double d) {
		String dec = String.format("%.17f", d);
		int last = dec.length() - 1;
		for (; last > 0 && dec.charAt(last) == '0'; --last);
		if (dec.charAt(last) == '.') {
			++last;
		}
		return dec.substring(0, last + 1);
	}
}
