package br.com.lsilva.mergejasperdemo.service;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import org.apache.commons.io.FilenameUtils;
import org.bouncycastle.jcajce.provider.symmetric.DES;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;

@Service
public class ReduceSizeService {

    public static final String DEST = "./src/main/resources/temp/reduce_size.pdf";
    public static final String SRC1 = "./src/main/resources/templates/pdf/eiarima.pdf";
    public static final String SRC2 = "./src/main/resources/templates/pdf/java8.pdf";
    public static final String SRC3 = "./src/main/resources/templates/pdf/teste.pdf";
    public static final String SRC4 = "./src/main/resources/templates/img/transferir.png";

    public void create() {
        File file = new File(DEST);
        file.getParentFile().mkdirs();
    }

    public File manipulatePdf() throws Exception {
        create();
        String[] IMAGES = { SRC4 };
        //Image image = new Image(ImageDataFactory.create(IMAGES[0]));
        PdfWriter writer = new PdfWriter(DEST, new WriterProperties().setFullCompressionMode(true));
        PdfDocument pdfDoc = new PdfDocument(writer);
//        Document doc = new Document(pdfDoc, new PageSize(image.getImageWidth(), image.getImageHeight()));
//
//        for (int i = 0; i < IMAGES.length; i++) {
//            image = new Image(ImageDataFactory.create(IMAGES[i]));
//            pdfDoc.addNewPage(new PageSize(image.getImageWidth(), image.getImageHeight()));
//            image.setFixedPosition(i + 1, 0, 0);
//            doc.add(image);
//        }
        //doc.close();
//        pdfDoc.close();
        float factor = 0.5f;

        for (PdfIndirectReference indRef : pdfDoc.listIndirectReferences()) {

            PdfObject pdfObject = indRef.getRefersTo();
            if (pdfObject == null || !pdfObject.isStream()) {
                continue;
            }

            PdfStream stream = (PdfStream) pdfObject;
            if (!PdfName.Image.equals(stream.getAsName(PdfName.Subtype))) {
                continue;
            }

            if (!PdfName.DCTDecode.equals(stream.getAsName(PdfName.Filter))) {
                continue;
            }

            PdfImageXObject pdfImageXObject = new PdfImageXObject(stream);
            BufferedImage origImage = pdfImageXObject.getBufferedImage();
            if (origImage == null) {
                continue;
            }

            int width = (int) (origImage.getWidth() * factor);
            int height = (int) (origImage.getHeight() * factor);
            if (width <= 0 || height <= 0) {
                continue;
            }

            // Scale the image
            BufferedImage resultImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            AffineTransform at = AffineTransform.getScaleInstance(factor, factor);
            Graphics2D graphics = resultImage.createGraphics();
            graphics.drawRenderedImage(origImage, at);
            ByteArrayOutputStream scaledBitmapStream = new ByteArrayOutputStream();
            ImageIO.write(resultImage, "PNG", scaledBitmapStream);

            resetImageStream(stream, scaledBitmapStream.toByteArray(), width, height);
            scaledBitmapStream.close();
        }
        pdfDoc.close();
        return new File(DEST);
    }

    private void resetImageStream(PdfStream stream, byte[] imgBytes, int imgWidth, int imgHeight) {
        stream.clear();
        stream.setData(imgBytes);
        stream.put(PdfName.Type, PdfName.XObject);
        stream.put(PdfName.Subtype, PdfName.Image);
        stream.put(PdfName.Filter, PdfName.DCTDecode);
        stream.put(PdfName.Width, new PdfNumber(imgWidth));
        stream.put(PdfName.Height, new PdfNumber(imgHeight));
        stream.put(PdfName.BitsPerComponent, new PdfNumber(8));
        stream.put(PdfName.ColorSpace, PdfName.DeviceRGB);
    }
}
