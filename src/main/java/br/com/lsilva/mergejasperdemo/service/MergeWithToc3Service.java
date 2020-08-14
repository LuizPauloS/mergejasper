package br.com.lsilva.mergejasperdemo.service;

import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Locale;

@Service
public class MergeWithToc3Service {

    public static final String DEST = "./src/main/resources/templates/temp/merge_with_toc2.pdf";
    public static final String SRC = "./src/main/resources/templates/pdf/eiarima.pdf";

    public void criaDiretorio() {
        File file = new File(DEST);
        file.getParentFile().mkdirs();
    }

    public File manipulatePdf() throws Exception {
        criaDiretorio();
        PdfDocument pdfDoc = new PdfDocument(new PdfReader(SRC), new PdfWriter(DEST));

        // Please note that we don't change the page size in this example, but only shrink the content (in this case to 80%)
        // and the content is shrunk to center of the page, leaving bigger margins to the top, bottom, left and right
        float percentage = 0.9f;
        for (int p = 1; p <= pdfDoc.getNumberOfPages(); p++) {
            PdfPage pdfPage = pdfDoc.getPage(p);
            Rectangle pageSize = pdfPage.getPageSize();

            // Applying the scaling in both X, Y direction to preserve the aspect ratio.
            float offsetX = (pageSize.getWidth() * (1 - percentage)) / 2;
            float offsetY = (pageSize.getHeight() * (1 - percentage)) / 2;

            // The content, placed on a content stream before, will be rendered before the other content
            // and, therefore, could be understood as a background (bottom "layer")
            new PdfCanvas(pdfPage.newContentStreamBefore(), pdfPage.getResources(), pdfDoc)
                    .writeLiteral(String.format(Locale.ENGLISH, "\nq %s 0 0 %s %s %s cm\nq\n",
                            percentage, percentage, offsetX, offsetY));

            // The content, placed on a content stream after, will be rendered after the other content
            // and, therefore, could be understood as a foreground (top "layer")
            new PdfCanvas(pdfPage.newContentStreamAfter(), pdfPage.getResources(), pdfDoc)
                    .writeLiteral("\nQ\nQ\n");
        }

        pdfDoc.close();
        return new File(DEST);
    }
}
