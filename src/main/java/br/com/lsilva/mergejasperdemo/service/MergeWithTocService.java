package br.com.lsilva.mergejasperdemo.service;

import br.com.lsilva.mergejasperdemo.model.Documento;
import br.com.lsilva.mergejasperdemo.model.ProcessoDigital;
import br.com.lsilva.mergejasperdemo.repository.ProcessoRepository;
import com.itextpdf.forms.PdfPageFormCopier;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.io.source.IRandomAccessSource;
import com.itextpdf.io.source.RandomAccessSourceFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.action.PdfAction;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.draw.DashedLine;
import com.itextpdf.kernel.pdf.navigation.PdfDestination;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.property.TabAlignment;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.VerticalAlignment;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class MergeWithTocService {

    @Autowired
    private ProcessoRepository repository;
    private static final String SRC_JASPER = "/templates/pdf/";
    private static final String DEST = "./src/main/resources/temp/";

    public String verificaSeDiretorioLocalExiste(String numeroProcesso) {
        String arquivoTemp = DEST + numeroProcesso + ".pdf";
        File file = new File(arquivoTemp);
        if (file.exists()) {
            file.delete();
        }
        return arquivoTemp;
    }

    public File manipulatePdf(Integer id) throws Exception {
        Optional<ProcessoDigital> optionalProcesso = this.repository.findById(id);

        if (!optionalProcesso.isPresent()) {
           throw new RuntimeException("Processo informado não encontrado na base de dados!");
        }

        String dest = verificaSeDiretorioLocalExiste(optionalProcesso.get().getNumeroProcesso());

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PdfDocument pdfDoc = new PdfDocument(new PdfWriter(baos));
        final Document doc = new Document(pdfDoc);

        // Copier contains the additional logic to copy acroform fields to a new page.
        // PdfPageFormCopier uses some caching logic which can potentially improve performance
        // in case of the reusing of the same instance.
        final PdfPageFormCopier formCopier = new PdfPageFormCopier();

        // Copy all merging file's pages to the temporary pdf file
        Map<Double, Map<String, PdfDocument>> filesToMerge = initializeFilesToMerge(optionalProcesso.get());
        Map<Integer, String> toc = new TreeMap<>();
        int page = 1;
        for (Map.Entry<Double, Map<String, PdfDocument>> entry : filesToMerge.entrySet()) {
            //PdfDocument srcDoc = entry.getValue();
            Map<String, PdfDocument> mapDocument = entry.getValue();
            for (Map.Entry<String, PdfDocument> entryDocument : mapDocument.entrySet()) {
                final PdfDocument srcDoc = entryDocument.getValue();
                int numberOfPages = srcDoc.getNumberOfPages();

                toc.put(page, entryDocument.getKey());

                for (int i = 1; i <= numberOfPages; i++, page++) {
                    Text text = new Text(String.format("%d", page));
                    srcDoc.getReader().setUnethicalReading(true);
                    srcDoc.copyPagesTo(i, i, pdfDoc, formCopier);

                    // Put the destination at the very first page of each merged document
                    if (i == 1) {
                        text.setDestination("p" + page);

                        final PdfOutline rootOutLine = pdfDoc.getOutlines(false);
                        final PdfOutline outline = rootOutLine.addOutline("p" + page);
                        outline.addDestination(PdfDestination.makeDestination(new PdfString("p" + page)));
                    }

                    doc.add(new Paragraph(text)
                            .setFixedPosition(page, 549, 810, 40)
                            .setMargin(0)
                            .setMultipliedLeading(1));
                }
            }
            for (PdfDocument srcDoc : mapDocument.values()) {
                srcDoc.close();
            }
        }

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final PdfWriter writer = new PdfWriter(outputStream, new WriterProperties().setPdfVersion(PdfVersion.PDF_2_0));
        writer.setSmartMode(true);
        PdfDocument tocDoc = new PdfDocument(writer);
        tocDoc.addNewPage();
        PdfPage pdfPage = tocDoc.getFirstPage();
        Text titulo = new Text("Índice ").setBold();
        PdfCanvas pdfCanvas = new PdfCanvas(pdfPage.newContentStreamBefore(), pdfPage.getResources(), tocDoc);
        new Canvas(pdfCanvas, tocDoc, pdfPage.getPageSize())
                .showTextAligned(titulo.getText(), 280, pdfPage.getPageSize().getTop() - 30, TextAlignment.CENTER,
                        VerticalAlignment.TOP, 0);
        tocDoc.close();

        tocDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(outputStream.toByteArray())));
        tocDoc.copyPagesTo(1, tocDoc.getNumberOfPages(), pdfDoc, formCopier);
        tocDoc.close();
        outputStream.close();

        // Create a table of contents
        float tocYCoordinate = 750;
        float tocXCoordinate = doc.getLeftMargin();
        float tocWidth = pdfDoc.getDefaultPageSize().getWidth() - doc.getLeftMargin() - doc.getRightMargin();
        for (Map.Entry<Integer, String> entry : toc.entrySet()) {
            Paragraph p = new Paragraph();
            p.addTabStops(new TabStop(500, TabAlignment.LEFT, new DashedLine()));
            p.add(entry.getValue());
            p.add(new Tab());
            p.add(String.valueOf(entry.getKey()));
            p.setAction(PdfAction.createGoTo("p" + entry.getKey()));
            doc.add(p.setFixedPosition(pdfDoc.getNumberOfPages(), tocXCoordinate, tocYCoordinate, tocWidth)
                    .setMargin(0).setMultipliedLeading(1));
            tocYCoordinate -= 20;
        }

        doc.close();

        final PdfDocument resultDoc = new PdfDocument(new PdfWriter(dest));
        final PdfDocument srcDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(baos.toByteArray()),
                new ReaderProperties()));
        srcDoc.initializeOutlines();

        // Create a copy order list and set the page with a table of contents as the first page
        int tocPageNumber = srcDoc.getNumberOfPages();
        List<Integer> copyPagesOrderList = new ArrayList<>();
        copyPagesOrderList.add(tocPageNumber);
        for(int i = 1; i < tocPageNumber; i++) {
            copyPagesOrderList.add(i);
        }

        srcDoc.copyPagesTo(copyPagesOrderList, resultDoc, formCopier);

        writer.close();
        srcDoc.close();
        resultDoc.close();
        pdfDoc.close();
        baos.close();
        return new File(dest);
    }

    private Map<Double, Map<String, PdfDocument>> initializeFilesToMerge(ProcessoDigital processo) throws Exception {
        TreeMap<Double, Map<String, PdfDocument>> filesToMerge = new TreeMap<>();
        filesToMerge.put(1.0, addReport("Requerimento Padrão", SRC_JASPER + "requerimento-padrao.pdf"));
        filesToMerge.put(1.1, addReport("Resumo Técnico da Atividade", SRC_JASPER + "resumo-tecnico.pdf"));
        //Filtrando listas para iniciar ordenação
        List<Documento> documentosPrioridade = processo.getDocumentos().stream()
                .filter(this::isRepeitarPrioridade).collect(Collectors.toList());
        List<Documento> documentosDataCriacao = processo.getDocumentos().stream()
                .filter(this::isNaoRepeitarPrioridade).sorted(Comparator.comparing(Documento::getDataCriacao))
                .collect(Collectors.toList());
        //Adiciona na lista documentos com prioridade
        addListDocumentsByPriority(documentosPrioridade, filesToMerge);
        //Adiciona na lista documentos sem prioridade mas ordenado pela data criação
        addListDocumentsByCreationDate(documentosDataCriacao, filesToMerge);
        return filesToMerge;
    }

    private Map<String, PdfDocument> addReport(String nomeDocumento, String pathDocumento) throws IOException {
        Map<String, PdfDocument> mapReqPadrao = new HashMap<>();
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(generatePDF(pathDocumento));
        mapReqPadrao.put(nomeDocumento, new PdfDocument(new PdfReader(byteArrayInputStream, new ReaderProperties())));
        byteArrayInputStream.close();
        return mapReqPadrao;
    }

    private byte[] generatePDF(String s) {
        try {
            return Files.readAllBytes(Paths.get(new ClassPathResource(s).getURI()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    private void addListDocumentsByPriority(List<Documento> documents,
                                            TreeMap<Double, Map<String, PdfDocument>> filesToMerge) throws IOException {
        for (int i = 0; i < documents.size(); i++) {
            Map<String, PdfDocument> map = new HashMap<>();
            final byte[] doc = generatePdfDocumentImage(documents.get(i));
            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(doc);
            map.put(documents.get(i).getNome(), new PdfDocument(new PdfReader(byteArrayInputStream, new ReaderProperties())));
            final Double key = Double.valueOf(documents.get(i).getPrioridade());
            filesToMerge.put(filesToMerge.containsKey(key) ? getKey(filesToMerge, key) : key, map);
            byteArrayInputStream.close();
        }
    }

    private double getKey(TreeMap<Double, Map<String, PdfDocument>> filesToMerge, Double key) {
        final List<Double> keys = filesToMerge.keySet().stream()
                .filter(isKeyInRange(key)).collect(Collectors.toList());
        return !keys.isEmpty() ? keys.stream().max(Double::compareTo)
                .orElseThrow(NoSuchElementException::new) + 0.1D : key + 0.1D;
    }

    private Predicate<Double> isKeyInRange(Double key) {
        return keyValue -> keyValue >= key && keyValue < (key + 1);
    }

    private void addListDocumentsByCreationDate(List<Documento> documents,
                                                TreeMap<Double, Map<String, PdfDocument>> filesToMerge) throws IOException {
        Double lastKey = filesToMerge.lastKey();
        if (!documents.isEmpty() && lastKey != null) {
            for (Documento documento: documents) {
                Map<String, PdfDocument> map = new HashMap<>();
                byte[] doc = generatePdfDocumentImage(documento);
                final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(doc);
                map.put(documento.getNome(), new PdfDocument(new PdfReader(byteArrayInputStream, new ReaderProperties())));
                filesToMerge.put(lastKey++, map);
                byteArrayInputStream.close();
            }
        }
    }

    private boolean isRepeitarPrioridade(Documento documento) {
        return !isNaoRepeitarPrioridade(documento);
    }

    private boolean isNaoRepeitarPrioridade(Documento documento) {
        return !documento.isRespeitarPrioridade();
    }

    private byte[] generateByteDocumentPDF(String arquivo) throws IOException {
        return Files.readAllBytes(new File(arquivo).toPath());
    }

    public byte[] generatePdfDocumentImage(Documento documento) throws IOException {
        final byte[] byteDocument = generateByteDocumentPDF(documento.getArquivo());

        String extensao = FilenameUtils.getExtension(documento.getArquivo());
        if (extensao != null && !(extensao.equalsIgnoreCase("png") ||
                extensao.equalsIgnoreCase("jpg"))) {
            float percentage = 0.8f;
            IRandomAccessSource source = new RandomAccessSourceFactory().createSource(byteDocument);
            final ByteArrayOutputStream result = new ByteArrayOutputStream();
            final PdfWriter pdfWriter = new PdfWriter(result, new WriterProperties().setPdfVersion(PdfVersion.PDF_2_0));
            final PdfReader pdfReader = new PdfReader(source, new ReaderProperties());
            pdfReader.setUnethicalReading(true);
            pdfWriter.setCompressionLevel(9);
            final PdfDocument copy = new PdfDocument(pdfReader, new PdfWriter(pdfWriter));
            for (int p = 1; p <= copy.getNumberOfPages(); p++) {
                PdfPage pdfPage = copy.getPage(p);
                Rectangle pageSize = pdfPage.getPageSize();

                // Applying the scaling in both X, Y direction to preserve the aspect ratio.
                float offsetX = (pageSize.getWidth() * (1 - percentage)) / 2;
                float offsetY = (pageSize.getHeight() * (1 - percentage)) / 2;

                // The content, placed on a content stream before, will be rendered before the other content
                // and, therefore, could be understood as a background (bottom "layer")
                new PdfCanvas(pdfPage.newContentStreamBefore(), pdfPage.getResources(), copy)
                        .writeLiteral(String.format(Locale.ENGLISH, "\nq %s 0 0 %s %s %s cm\nq\n",
                                percentage, percentage, offsetX, offsetY));

                // The content, placed on a content stream after, will be rendered after the other content
                // and, therefore, could be understood as a foreground (top "layer")
                new PdfCanvas(pdfPage.newContentStreamAfter(), pdfPage.getResources(), copy)
                        .writeLiteral("\nQ\nQ\n");
            }
            pdfWriter.close();
            copy.close();
            return result.toByteArray();
        }

        final ByteArrayOutputStream result = new ByteArrayOutputStream();
        final PdfWriter pdfWriter = new PdfWriter(result, new WriterProperties().setPdfVersion(PdfVersion.PDF_2_0));
        final PdfDocument copy = new PdfDocument(new PdfWriter(pdfWriter));
        final Document document = new Document(copy);

        float leftMargin = document.getLeftMargin(), rightMargin = document.getRightMargin();
        float topMargin = document.getTopMargin(), bottomMargin = document.getBottomMargin();
        float pdfA4usableWidth = PageSize.A4.getWidth() - leftMargin - rightMargin;
        float pdfA4usableHeight = PageSize.A4.getHeight() - topMargin - bottomMargin;

        ImageData imageData = ImageDataFactory.create(byteDocument);
        Image img = new Image(imageData);
        img.scaleToFit(pdfA4usableWidth, pdfA4usableHeight);
        float x = (PageSize.A4.getWidth() - img.getImageScaledWidth()) / 2;
        float y = (PageSize.A4.getHeight() - img.getImageScaledHeight()) / 2;
        img.setFixedPosition(1, x, y);

        document.add(img);
        document.close();
        copy.close();
        pdfWriter.close();
        return result.toByteArray();
    }

}
