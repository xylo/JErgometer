package velocity;

import de.endrullis.utils.VelocityUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

import java.io.StringWriter;
import java.io.Writer;

/**
 * Hello World using Velocity.
 */
public class HelloWorld {
	public static void main(String[] args) throws Exception {
		// get the template
		Template template = VelocityUtils.getTemplate("templates/HelloWorld.vm");
		// create a context for Velocity
		VelocityContext context = new VelocityContext();
		//EventCartridge ec = new EventCartridge();
		//ec.addEventHandler(new TryInsertionHandler());
		//ec.attachToContext( context );
		context.put("message", "Hello World");
		context.put("message2", null);
		context.put("message3", "Hi");
		// create the output
		Writer writer = new StringWriter();
		template.merge(context, writer);
		// write out
		System.out.println(writer.toString());
		//org.apache.velocity.runtime.directive.Foreach
	}
}
