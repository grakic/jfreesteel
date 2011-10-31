package net.devbase.jfreesteel.viewer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import net.devbase.jfreesteel.EidInfo;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;

public class PdfReport
{
	java.awt.Image photo;
	EidInfo info;
	
    protected final String REPORT = getClass().getResource("/net/devbase/jfreesteel/viewer/report.pdf").toString();

    public PdfReport(EidInfo info, java.awt.Image photo)
    {
		this.info = info;
		this.photo = photo;
	}
    
    /**
     * Creates a PDF with information about the movies
     * @param    filename the name of the PDF file that will be created.
     * @throws    DocumentException 
     * @throws    IOException
     */
    public void write(final String filename) throws IOException, DocumentException
    {

    	Document document = new Document();
    	document.setPageSize(PageSize.A4);
    	PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filename));

        PdfReader reader = new PdfReader(REPORT);
        PdfImportedPage page = writer.getImportedPage(reader, 1);
        
    	document.open();
    	
    	// Read from byte stream, as otherwise photo gets wash out
    	ByteArrayOutputStream bas = new ByteArrayOutputStream();
    	ImageIO.write((BufferedImage) photo, "jpeg", bas);
    	byte[] data = bas.toByteArray();    		
        Image image = Image.getInstance(data);

        image.setAbsolutePosition(60, 571);
        image.scaleAbsolute(119, 158);
        writer.getDirectContent().addImage(image);
        
        writer.getDirectContentUnder().addTemplate(page, 0, 0);
        document.close();
    }
}
