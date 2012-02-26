package de.endrullis.utils;

import org.apache.velocity.Template;
import org.apache.velocity.app.Velocity;

import java.io.InputStream;
import java.util.Properties;

/**
 * Velocity utilities.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class VelocityUtils {
	private static boolean initialized = false;

	public static void init() throws Exception {
		if (initialized) return;

		// initialize Velocity
		InputStream inputStream = StreamUtils.getInputStream("velocity.properties");
		Properties properties = new Properties();
		properties.load(inputStream);
		Velocity.init(properties);

		initialized = true;
	}


	public static Template getTemplate(String name) throws Exception {
		init();

		// get the template
		return Velocity.getTemplate(name);
	}
}
