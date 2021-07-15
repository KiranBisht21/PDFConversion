package pdfconverter.core.servlets;

import org.apache.commons.codec.CharEncoding;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.engine.SlingRequestProcessor;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import com.day.cq.contentsync.handler.util.RequestResponseFactory;
import com.day.cq.wcm.api.WCMMode;
import org.w3c.dom.Document;
import org.xhtmlrenderer.pdf.ITextRenderer;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

@Component(service = Servlet.class, property = {
        "sling.servlet.resourceTypes=" + "sling/servlet/default",
        "sling.servlet.selectors=" + "pdf",
        "sling.servlet.extensions=" + "html"})


public class GeneratePdfServlet extends SlingSafeMethodsServlet {
    @Reference
    private RequestResponseFactory requestResponseFactory;
    @Reference
    private SlingRequestProcessor requestProcessor;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        final String filePath= request.getResource().getPath();
        String ext = request.getRequestPathInfo().getExtension();
        response.getWriter().println("PDF Document!!!!!!!");
        String finalPath= filePath +"."+ext;

        if ("html".equalsIgnoreCase(ext)) {
            String htmlString = extractHTML(request, finalPath);
            response.getWriter().println(htmlString);
            outputPDF(htmlString,response, request.getResourceResolver());
        }
        response.getWriter().println("PDF Document Ended!!!!!!!"+" "+finalPath);

    }

    //Get the rendered HTML for an AEM page
    public String extractHTML(SlingHttpServletRequest request, String finalPath) throws IOException, ServletException {
        HttpServletRequest req = requestResponseFactory.createRequest("GET", finalPath);
        WCMMode.DISABLED.toRequest(req);
        ByteArrayOutputStream out= new ByteArrayOutputStream();
        HttpServletResponse resp = requestResponseFactory.createResponse(out);
        requestProcessor.processRequest(req, resp, request.getResourceResolver());
        return out.toString(CharEncoding.UTF_8);
    }

        //Convert rendered HTML to PDF
    public  void outputPDF(String htmlString, SlingHttpServletResponse response, ResourceResolver resourceResolver) throws IOException {
        OutputStream os = response.getOutputStream();
        ITextRenderer renderer = new ITextRenderer();
        org.jsoup.nodes.Document document = Jsoup.parse(htmlString);//With the Jsoup's parse method, we parse the HTML string. The method returns a HTML document.


        W3CDom w3cDom = new W3CDom();
        Document doc = w3cDom.fromJsoup(document); //Convert a jsoup Document to a W3C Document.
        renderer.setDocumentFromString(doc.toString());
        renderer.layout();
        try {
            renderer.createPDF(os);
        } catch (com.lowagie.text.DocumentException e) {
            e.printStackTrace();
        }
        // complete the PDF
        renderer.finishPDF();
        // saving the PDF
        response.setHeader("Expires", "0");  //Expires header specifies when content will expire, or how long content is “fresh.”
        response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");  // disable browser caching
        response.setHeader("Pragma", "public"); //public responses can be cached
        // setting the content type
        response.setContentType("application/pdf");
        response.setHeader("Content-disposition", "attachment; filename=Sample.pdf"); //export pdf
        os.flush();
        os.close();
    }

}




