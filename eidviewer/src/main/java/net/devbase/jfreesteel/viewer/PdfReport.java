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
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * Added info printing
 *
 * @edited_by Svetislav Marković <svetam.sd@gmail.com>
 * @date 06.01.2014
 */
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
        
        image.setAbsolutePosition(58, 570);
        image.scaleAbsolute(123, 165);
        writer.getDirectContent().addImage(image);
        
        // Write info
        PdfContentByte cb = writer.getDirectContent();
        writeTextToPdfContentByte(cb, info.getSurname(), 200, 513);
        writeTextToPdfContentByte(cb, info.getGivenName(), 200, 489);
        writeTextToPdfContentByte(cb, info.getParentGivenName(), 200, 463);
        writeTextToPdfContentByte(cb, info.getDateOfBirth(), 200, 440);
        writeTextToPdfContentByte(cb, info.getPlaceOfBirthFull().replace("\n", ", "), 200, 408);
        writeTextToPdfContentByte(cb,
                                  info.getStreet() + ", "
                                  + info.getHouseNumber() + ", "
                                  + info.getCommunity() + ", "
                                  + info.getPlace() + ", "
                                  + info.getState(), 200, 370);
        writeTextToPdfContentByte(cb, info.getPersonalNumber(), 200, 340);
        writeTextToPdfContentByte(cb, info.getSex(), 200, 316);
        writeTextToPdfContentByte(cb, info.getIssuingAuthority(), 200, 267);
        writeTextToPdfContentByte(cb, info.getDocRegNo(), 200, 243);
        writeTextToPdfContentByte(cb, info.getIssuingDate(), 200, 220);
        writeTextToPdfContentByte(cb, info.getExpiryDate(), 200, 195);
        
        writer.getDirectContentUnder().addTemplate(page, 0, 0);
        
        document.close();
    }
    
    /**
     * Used to write text into PdfContentByte
     *
     * @param cb PDF Content
     * @param text String to be drawn
     * @param x X Position
     * @param y Y Position
     * @throws DocumentException
     * @throws IOException
     * @author Svetislav Marković <svetam.sd@gmail.com>
     */
    private void writeTextToPdfContentByte(PdfContentByte cb, String text, int x, int y) throws DocumentException, IOException {
        String fontPath = getClass().getResource("/net/devbase/jfreesteel/viewer/arialuni.ttf").toString();
        BaseFont bf = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
        cb.beginText();
        cb.setTextMatrix(x, y);
        cb.setFontAndSize(bf, 10);
        cb.showText(text);
        cb.endText();
    }
}
