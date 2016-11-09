package by.knick.eli;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class TextRenderServiceTest {

	private TextRenderService testable = new TextRenderServiceImpl();
	
	@Test
	public void renderHello() throws IOException{
		byte[] imgBytes = testable.renderText("Hello World");
		File imgBytesFile = File.createTempFile("text-render-unittest-renderHello", ".png");
		FileUtils.writeByteArrayToFile(imgBytesFile, imgBytes);
	}
}
