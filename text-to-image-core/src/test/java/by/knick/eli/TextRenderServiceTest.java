package by.knick.eli;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

public class TextRenderServiceTest {

	private TextRenderService testable = new TextRenderServiceImpl();

	private String textExample = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec libero sapien, lacinia vitae aliquet eu, interdum quis velit. Vivamus ullamcorper nibh non tincidunt faucibus. Ut vel nisi at dui elementum ornare. Fusce nec dictum est, sed semper mi. Cras eget enim eget sem rhoncus eleifend. Nulla facilisi. Donec ut nulla arcu. Quisque tempor semper mi. Etiam commodo tincidunt sodales. Vivamus vulputate est vel nisi finibus, et mattis ex pellentesque. Proin eget urna nec leo auctor condimentum nec quis erat. Sed condimentum neque arcu, a laoreet mi imperdiet vitae. Maecenas rhoncus augue non rutrum tempor. Aliquam at venenatis augue, et lacinia nunc. Aliquam erat volutpat. Nullam at sem vehicula, laoreet eros ac, malesuada dolor. Integer molestie tincidunt tortor at semper. Cras euismod condimentum tortor, vitae cursus nulla gravida eget. Praesent nulla mi, rhoncus ultricies mi in, ultricies ultricies nisi. Proin consequat nisl id ultrices pretium. Cras in elit vel nibh ullamcorper volutpat vel in ipsum. Praesent porttitor dignissim sem ut scelerisque. Suspendisse ut pellentesque lectus. Proin sit amet fermentum justo. Aliquam sodales at justo sit amet ultrices. Mauris viverra, turpis vulputate convallis laoreet, odio tortor finibus odio, in porta mauris arcu at leo.";

	@Test
	public void render() throws IOException {
		int charCount = 1;
		do {
			String text = textExample.substring(0, charCount);

			byte[] imgBytes = testable.renderText(text);
			Assert.assertNotNull(imgBytes);

			File imgBytesFile = File.createTempFile("text-render-unittest-renderHello-", ".png");
			FileUtils.writeByteArrayToFile(imgBytesFile, imgBytes);

			charCount = (int)(charCount * 1.5) + 1;
		} while (charCount <= textExample.length());
	}
}
