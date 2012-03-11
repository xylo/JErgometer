package de.endrullis.utils;

import org.apache.velocity.Template;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.ResourceNotFoundException;

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

		InputStream resourceAsStream = VelocityUtils.class.getResourceAsStream("/" + name);
		if (resourceAsStream == null) throw new ResourceNotFoundException("Resource " + name + " could not be found");

		// get the template
		return Velocity.getTemplate(name);
	}
}
