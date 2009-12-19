package org.jergometer.translation;

import de.endrullis.utils.Utf8ResourceBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

import java.util.ResourceBundle;

/**
 * I18n support for the project jergometer.
 */
public class I18n {
  @NonNls
  public static final ResourceBundle bundle = Utf8ResourceBundle.getBundle("org.jergometer.translation.jergometer");

  public static String getString(@PropertyKey(resourceBundle = "org.jergometer.translation.jergometer") String key, Object... params) {
    String value = bundle.getString(key);
    if (params.length == 0) {
      return value;
    } else {
      return String.format(value, params);
    }
  }

  public static char getMnemonic(@PropertyKey(resourceBundle = "org.jergometer.translation.jergometer") String key) {
    String value = getString(key);
    if (value == null) {
      return '!';
    }
    return value.charAt(0);
  }
}
