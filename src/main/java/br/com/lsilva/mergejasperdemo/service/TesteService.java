package br.com.lsilva.mergejasperdemo.service;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.action.PdfAction;
import com.itextpdf.kernel.pdf.canvas.draw.DottedLine;
import com.itextpdf.kernel.pdf.navigation.PdfDestination;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.layout.LayoutContext;
import com.itextpdf.layout.layout.LayoutResult;
import com.itextpdf.layout.property.TabAlignment;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.VerticalAlignment;
import com.itextpdf.layout.renderer.ParagraphRenderer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.AbstractMap.SimpleEntry;

@Service
public class TesteService {

    public static final String IMG_SRC = "/templates/img/";
    public static final String SRC = "./src/main/resources/templates/pdf/teste.txt";

    public List<byte[]> getImagesFormat() throws IOException {
        byte[] img = Files.readAllBytes(Paths.get(new ClassPathResource(IMG_SRC + "cpf.jpg").getURI()));
        byte[] img2 = Files.readAllBytes(Paths.get(new ClassPathResource(IMG_SRC + "rg.jpg").getURI()));
        byte[] img3 = Files.readAllBytes(Paths.get(new ClassPathResource(IMG_SRC + "brasao_ms.jpg").getURI()));
        byte[] img4 = Files.readAllBytes(Paths.get(new ClassPathResource(IMG_SRC + "logo.png").getURI()));
        byte[] img5 = Files.readAllBytes(Paths.get(new ClassPathResource(IMG_SRC + "logo_governo.png").getURI()));
        byte[] img6 = Files.readAllBytes(Paths.get(new ClassPathResource(IMG_SRC + "logo_imasul.jpg").getURI()));
        byte[] img7 = Files.readAllBytes(Paths.get(new ClassPathResource(IMG_SRC + "relatorio_logo.jpg").getURI()));
        byte[] img8 = Files.readAllBytes(Paths.get(new ClassPathResource(IMG_SRC + "teste.jpg").getURI()));

        return Arrays.asList(img, img2, img3, img4, img5, img6, img7, img8);
    }

    public byte[] getPdfDocumentImage() throws IOException {
        List<byte[]> images = getImagesFormat();
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        PdfWriter pdfWriter = new PdfWriter(result);
        PdfDocument copy = new PdfDocument(new PdfWriter(pdfWriter));
        Document document = new Document(copy);

        float leftMargin = document.getLeftMargin(), rightMargin = document.getRightMargin();
        float topMargin = document.getTopMargin(), bottomMargin = document.getBottomMargin();
        float pdfA4usableWidth = PageSize.A4.getWidth() - leftMargin - rightMargin;
        float pdfA4usableHeight = PageSize.A4.getHeight() - topMargin - bottomMargin;

        for (int i = 0; i < images.size(); i++) {
            ImageData imageData = ImageDataFactory.create(images.get(i));
            Image img = new Image(imageData);
            img.scaleToFit(pdfA4usableWidth, pdfA4usableHeight);
            float x = (PageSize.A4.getWidth() - img.getImageScaledWidth()) / 2;
            float y = (PageSize.A4.getHeight() - img.getImageScaledHeight()) / 2;
            img.setFixedPosition(i+1, x, y);
            document.add(img);
        }

        int numberOfPages = copy.getNumberOfPages();
        for (int i = 1; i <= numberOfPages; i++) {
            // Write aligned text to the specified by parameters point
            document.showTextAligned(new Paragraph(String.format("page %s of %s", i, numberOfPages)),
                    559, 806, i, TextAlignment.RIGHT, VerticalAlignment.TOP, 0);
        }

        document.close();
        return result.toByteArray();
    }

    public byte[] manipulatePdf() throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PdfFont font = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
        PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfDocument pdfDoc = new PdfDocument(new PdfWriter(byteArrayOutputStream));
        Document document = new Document(pdfDoc);
        document.setTextAlignment(TextAlignment.JUSTIFIED)
                .setFont(font)
                .setFontSize(11);
        List<SimpleEntry<String, SimpleEntry<String, Integer>>> toc = new ArrayList<>();

        // Parse text to PDF
        createPdfWithOutlines(SRC, document, toc, bold);

        // Remove the main title from the table of contents list
        toc.remove(0);

        // Create table of contents
        document.add(new AreaBreak());
        Paragraph p = new Paragraph("Table of Contents")
                .setFont(bold)
                .setDestination("toc");
        document.add(p);
        List<TabStop> tabStops = new ArrayList<>();
        tabStops.add(new TabStop(580, TabAlignment.RIGHT, new DottedLine()));
        for (SimpleEntry<String, SimpleEntry<String, Integer>> entry : toc) {
            SimpleEntry<String, Integer> text = entry.getValue();
            p = new Paragraph()
                    .addTabStops(tabStops)
                    .add(text.getKey())
                    .add(new Tab())
                    .add(String.valueOf(text.getValue()))
                    .setAction(PdfAction.createGoTo(entry.getKey()));
            document.add(p);
        }

        // Move the table of contents to the first page
        int tocPageNumber = pdfDoc.getNumberOfPages();
        pdfDoc.movePage(tocPageNumber, 1);

        // Add page labels
        pdfDoc.getPage(1).setPageLabel(PageLabelNumberingStyle.UPPERCASE_LETTERS,
                null, 1);
        pdfDoc.getPage(2).setPageLabel(PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS,
                null, 1);

        document.close();
        return byteArrayOutputStream.toByteArray();
    }

    private static void createPdfWithOutlines(String path, Document document,
                                              List<SimpleEntry<String, SimpleEntry<String, Integer>>> toc, PdfFont titleFont) throws Exception {
        PdfDocument pdfDocument = document.getPdfDocument();

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            boolean title = true;
            int counter = 0;
            PdfOutline outline = null;
            while ((line = br.readLine()) != null) {
                Paragraph p = new Paragraph(line);
                p.setKeepTogether(true);
                if (title) {
                    String name = String.format("title%02d", counter++);
                    outline = createOutline(outline, pdfDocument, line, name);
                    SimpleEntry<String, Integer> titlePage = new SimpleEntry(line, pdfDocument.getNumberOfPages());
                    p
                            .setFont(titleFont)
                            .setFontSize(12)
                            .setKeepWithNext(true)
                            .setDestination(name)

                            // Add the current page number to the table of contents list
                            .setNextRenderer(new UpdatePageRenderer(p, titlePage));
                    document.add(p);
                    toc.add(new SimpleEntry(name, titlePage));
                    title = false;
                } else {
                    p.setFirstLineIndent(36);
                    if (line.isEmpty()) {
                        p.setMarginBottom(12);
                        title = true;
                    } else {
                        p.setMarginBottom(0);
                    }

                    document.add(p);
                }
            }
        }
    }

    private static PdfOutline createOutline(PdfOutline outline, PdfDocument pdf, String title, String name) {
        if (outline == null) {
            outline = pdf.getOutlines(false);
            outline = outline.addOutline(title);
            outline.addDestination(PdfDestination.makeDestination(new PdfString(name)));
        } else {
            PdfOutline kid = outline.addOutline(title);
            kid.addDestination(PdfDestination.makeDestination(new PdfString(name)));
        }

        return outline;
    }

    private static class UpdatePageRenderer extends ParagraphRenderer {
        protected SimpleEntry<String, Integer> entry;

        public UpdatePageRenderer(Paragraph modelElement, SimpleEntry<String, Integer> entry) {
            super(modelElement);
            this.entry = entry;
        }

        @Override
        public LayoutResult layout(LayoutContext layoutContext) {
            LayoutResult result = super.layout(layoutContext);
            entry.setValue(layoutContext.getArea().getPageNumber());
            return result;
        }
    }
}
