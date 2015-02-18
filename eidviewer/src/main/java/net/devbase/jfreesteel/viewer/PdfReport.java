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
import com.itextpdf.text.pdf.PdfWriter;

/**
 * Added info printing
 *
 * @edited_by Svetislav Marković <svetam.sd@gmail.com>
 */
public class PdfReport
{
    java.awt.Image photo;
    EidInfo info;

    public PdfReport(EidInfo info, java.awt.Image photo)
    {
        this.info = info;
        this.photo = photo;
    }

    /**
     * Creates a PDF with information about the movies
     * 
     * @param filename the name of the PDF file that will be created.
     * @throws DocumentException
     * @throws IOException
     */
    public void write(final String filename) throws IOException, DocumentException
    {

        Document document = new Document();
        document.setPageSize(PageSize.A4);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filename));
        document.open();

        // Write image: Read from byte stream, as otherwise photo gets wash out
        ByteArrayOutputStream bas = new ByteArrayOutputStream();
        ImageIO.write((BufferedImage) photo, "jpeg", bas);
        byte[] data = bas.toByteArray();
        Image image = Image.getInstance(data);
        image.setAbsolutePosition(60, 572);
        image.setBorder(Image.BOX);
        image.setBorderWidth(1f);
        image.scaleAbsolute(119, 158);
        writer.getDirectContent().addImage(image);

        // Write info
        PdfContentByte cb = writer.getDirectContent();

        drawRulers(cb, 2f, 782, 747);
        drawRulers(cb, 1.5f, 554, 529, 304, 279);

        String fontPath = getClass().getResource("/net/devbase/jfreesteel/viewer/DejaVuSans.ttf").toString();
        BaseFont bf = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        cb.beginText();

        cb.setFontAndSize(bf, 15);
        writeText(cb, "ЧИТАЧ  ЕЛЕКТРОНСКЕ  ЛИЧНЕ  КАРТЕ: ШТАМПА  ПОДАТАКА", 62, 760);

        cb.setFontAndSize(bf, 11);
        writeLabel(cb, "Подаци о грађанину", 537);
        writeLabel(cb, "Подаци о документу", 288);

        cb.setFontAndSize(bf, 10);
        writeLine(cb, "Презиме:", info.getSurname(), 513);
        writeLine(cb, "Име:", info.getGivenName(), 489);
        writeLine(cb, "Име једног родитеља:", info.getParentGivenName(), 463);
        writeLine(cb, "Датум рођења:", info.getDateOfBirth(), 440);
        writeLabel(cb, "Место рођења,", 415);
        writeLabel(cb, "општина и држава:", 403);
        writeLine(cb, "", info.getPlaceOfBirthFull().replace("\n", ", "), 409);
        writeLabel(cb, "Пребивалиште и", 380);
        writeLabel(cb, "адреса стана:", 368);
        writePlace(cb, info);
        writeLine(cb, "ЈМБГ:", info.getPersonalNumber(), 340);
        writeLine(cb, "Пол:", info.getSex(), 316);

        writeLine(cb, "Документ издаје:", info.getIssuingAuthority(), 262);
        writeLine(cb, "Број документа:", info.getDocRegNo(), 238);
        writeLine(cb, "Датум издавања:", info.getIssuingDate(), 215);
        writeLine(cb, "Важи до:", info.getExpiryDate(), 190);

        cb.endText();

        document.close();
    }

    private void writePlace(PdfContentByte cb, EidInfo info) throws DocumentException, IOException {

        String place[] = info.getPlaceFull("/ %s", "%s. sprat", "stan %s").split("\n");

        if (place.length > 1) {
            for(int i=2; i<place.length; i++)
                place[1] += ", " + place[i];

            writeLine(cb, "", place[0], 380);
            writeLine(cb, "", place[1], 368);
        } 
        else {
            writeLine(cb, "", place[0], 374);
        }
    }

    private void drawRulerLine(PdfContentByte cb, int height)
    {
        cb.moveTo(59, height);
        cb.lineTo(536, height);
        cb.stroke();
    }

    private void drawRulers(PdfContentByte cb, float weight, int... heights)
    {
        cb.setLineWidth(weight);
        for (int height : heights)
            drawRulerLine(cb, height);
    }

    /**
     * Write text into PdfContentByte
     *
     * @param cb PDF Content
     * @param text String to be drawn
     * @param height Y Position
     * @throws DocumentException
     * @throws IOException
     */
    private void writeText(PdfContentByte cb, String text, int x, int y) throws DocumentException, IOException
    {
        cb.setTextMatrix(x, y);
        cb.showText(text);
    }

    private void writeLabel(PdfContentByte cb, String text, int height) throws DocumentException, IOException
    {
        writeText(cb, text, 68, height);
    }

    private void writeLine(PdfContentByte cb, String label, String text, int height) throws DocumentException, IOException
    {
        writeLabel(cb, label, height);
        writeText(cb, text, 200, height);
    }

}
