package jmri.jmrit;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.swing.JFileChooser;
import jmri.util.FileUtil;
import jmri.util.JmriLocalEntityResolver;
import jmri.util.NoArchiveFileFilter;
import org.jdom2.Comment;
import org.jdom2.DocType;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.ProcessingInstruction;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle common aspects of XML files.
 * <P>
 * JMRI needs to be able to operate offline, so it needs to store resources
 * locally. At the same time, we want XML files to be transportable, and to have
 * their schema and stylesheets accessible via the web (for browser rendering).
 * Further, our code assumes that default values for attributes will be
 * provided, and it's necessary to read the schema for that to work.
 * <p>
 * We implement this using our own EntityResolver, the
 * {@link jmri.util.JmriLocalEntityResolver} class.
 *
 * @author	Bob Jacobsen Copyright (C) 2001, 2002, 2007, 2012, 2014
 */
public abstract class XmlFile {

    /**
     * Define root part of URL for XSLT style page processing instructions.
     * <p>
     * See the <A
     * HREF="http://jmri.org/help/en/html/doc/Technical/XmlUsage.shtml#xslt">XSLT
     * versioning discussion</a>.
     * <p>
     * Things that have been tried here: <dl>
     * <dt>/xml/XSLT/ <dd>(Note leading slash) Works if there's a copy of the
     * xml directory at the root of whatever served the XML file, e.g. the JMRI
     * web site or a local computer running a server. Doesn't work for e.g.
     * yahoo groups files. <dt>http://jmri.org/xml/XSLT/ <dd>Works well for
     * files on the JMRI.org web server, but only that. </dl>
     */
    public static final String xsltLocation = "/xml/XSLT/";

    /**
     * Read the contents of an XML file from its filename. The name is expanded
     * by the {@link #findFile} routine. If the file is not found, attempts to
     * read the XML file from a JAR resource.
     *
     * @param name Filename, as needed by {@link #findFile}
     * @throws org.jdom2.JDOMException
     * @throws java.io.FileNotFoundException
     * @return null if not found, else root element of located file
     */
    public Element rootFromName(String name) throws JDOMException, IOException {
        File fp = findFile(name);
        if (fp != null && fp.exists() && fp.canRead()) {
            if (log.isDebugEnabled()) {
                log.debug("readFile: " + name + " from " + fp.getAbsolutePath());
            }
            return rootFromFile(fp);
        }
        URL resource = FileUtil.findURL(name);
        if (resource != null) {
            return this.rootFromURL(resource);
        } else {
            if (!name.startsWith("xml")) {
                return this.rootFromName("xml" + File.separator + name);
            }
            log.warn("Did not find file or resource " + name);
            throw new FileNotFoundException("Did not find file or resource " + name);
        }
    }

    /**
     * Read a File as XML, and return the root object.
     *
     * Exceptions are only thrown when local recovery is impossible.
     *
     * @throws org.jdom2.JDOMException       only when all methods have failed
     * @throws java.io.FileNotFoundException
     * @param file File to be parsed. A FileNotFoundException is thrown if it
     *             doesn't exist.
     * @return root element from the file. This should never be null, as an
     *         exception should be thrown if anything goes wrong.
     */
    public Element rootFromFile(File file) throws JDOMException, IOException {
        if (log.isDebugEnabled()) {
            log.debug("reading xml from file: " + file.getPath());
        }

        FileInputStream fs = new FileInputStream(file);
        try {
            return getRoot(verify, fs);
        } finally {
            fs.close();
        }
    }

    /**
     * Read an {@link java.io.InputStream} as XML, and return the root object.
     *
     * Exceptions are only thrown when local recovery is impossible.
     *
     * @throws org.jdom2.JDOMException       only when all methods have failed
     * @throws java.io.FileNotFoundException
     * @param stream InputStream to be parsed.
     * @return root element from the file. This should never be null, as an
     *         exception should be thrown if anything goes wrong.
     */
    public Element rootFromInputStream(InputStream stream) throws JDOMException, IOException {
        return getRoot(verify, stream);
    }

    /**
     * Read a URL as XML, and return the root object.
     *
     * Exceptions are only thrown when local recovery is impossible.
     *
     * @throws org.jdom2.JDOMException only when all methods have failed
     * @throws FileNotFoundException
     * @param url URL locating the data file
     * @return root element from the file. This should never be null, as an
     *         exception should be thrown if anything goes wrong.
     */
    public Element rootFromURL(URL url) throws JDOMException, IOException {
        if (log.isDebugEnabled()) {
            log.debug("reading xml from URL: " + url.toString());
        }
        return getRoot(verify, url.openConnection().getInputStream());
    }
    /**
     * Specify a standard prefix for DTDs in new XML documents
     */
    static public final String dtdLocation = "/xml/DTD/";

    /**
     * Get the root element from an XML document in a stream.
     *
     * @param verify true if the XML document should be validated against its
     *               schema
     * @param stream input containing the XML document
     * @return the root element of the XML document
     * @throws org.jdom2.JDOMException if the XML document is invalid
     * @throws java.io.IOException     if the input cannot be read
     */
    protected Element getRoot(boolean verify, InputStream stream) throws JDOMException, IOException {
        log.trace("getRoot from stream");

        SAXBuilder builder = getBuilder(verify);  // argument controls validation
        Document doc = builder.build(new BufferedInputStream(stream));
        doc = processInstructions(doc);  // handle any process instructions
        // find root
        return doc.getRootElement();
    }

    /**
     * Get the root element from an XML document in a Reader.
     *
     * Runs through a BufferedReader for increased performance.
     *
     *
     * @param verify true if the XML document should be validated against its
     *               schema
     * @param reader input containing the XML document
     * @return the root element of the XML document
     * @throws org.jdom2.JDOMException if the XML document is invalid
     * @throws java.io.IOException     if the input cannot be read
     * @since 3.1.5
     */
    protected Element getRoot(boolean verify, InputStreamReader reader) throws JDOMException, IOException {
        log.trace("getRoot from reader with encoding {}", reader.getEncoding());
            
        SAXBuilder builder = getBuilder(verify);  // argument controls validation
        Document doc = builder.build(new BufferedReader(reader));
        doc = processInstructions(doc);  // handle any process instructions
        // find root
        return doc.getRootElement();
    }

    /**
     * Write a File as XML.
     *
     * @throws FileNotFoundException
     * @param file File to be created.
     * @param doc  Document to be written out. This should never be null.
     */
    public void writeXML(File file, Document doc) throws IOException, FileNotFoundException {
        // ensure parent directory exists
        if (file.getParent() != null) {
            FileUtil.createDirectory(file.getParent());
        }
        // write the result to selected file
        FileOutputStream o = new FileOutputStream(file);
        try {
            XMLOutputter fmt = new XMLOutputter();
            fmt.setFormat(Format.getPrettyFormat()
                    .setLineSeparator(System.getProperty("line.separator"))
                    .setTextMode(Format.TextMode.TRIM_FULL_WHITE));
            fmt.output(doc, o);
            o.flush();
        } finally {
            o.close();
        }
    }

    /**
     * Check if a file of the given name exists. This uses the same search order
     * as {@link #findFile}
     *
     * @param name file name, either absolute or relative
     * @return true if the file exists in a searched place
     */
    protected boolean checkFile(String name) {
        File fp = new File(name);
        if (fp.exists()) {
            return true;
        }
        fp = new File(FileUtil.getUserFilesPath() + name);
        if (fp.exists()) {
            return true;
        } else {
            File fx = new File(xmlDir() + name);
            return fx.exists();
        }
    }

    /**
     * Return a File object for a name. This is here to implement the search
     * rule: <OL> <LI>Look in user preferences directory, located by
     * {@link jmri.util.FileUtil#getUserFilesPath()} <li>Look in current working
     * directory (usually the JMRI distribution directory) <li>Look in program
     * directory, located by {@link jmri.util.FileUtil#getProgramPath()}
     * <LI>Look in XML directory, located by {@link #xmlDir} <LI>Check for
     * absolute name. </OL>
     *
     * @param name Filename perhaps containing subdirectory information (e.g.
     *             "decoders/Mine.xml")
     * @return null if file found, otherwise the located File
     */
    protected File findFile(String name) {
        URL url = FileUtil.findURL(name,
                FileUtil.getUserFilesPath(),
                ".",
                FileUtil.getProgramPath(),
                xmlDir());
        if (url != null) {
            try {
                return new File(url.toURI());
            } catch (URISyntaxException ex) {
                return null;
            }
        }
        return null;
    }

    /**
     * Diagnostic printout of as much as we can find
     *
     * @param name Element to print, should not be null
     */
    @SuppressWarnings("unchecked")
    static public void dumpElement(Element name) {
        List<Element> l = name.getChildren();
        for (Element l1 : l) {
            System.out.println(" Element: " + l1.getName() + " ns: " + l1.getNamespace());
        }
    }

    /**
     * Move original file to a backup. Use this before writing out a new version
     * of the file.
     *
     * @param name Last part of file pathname i.e. subdir/name, without the
     *             pathname for either the xml or preferences directory.
     */
    public void makeBackupFile(String name) {
        File file = findFile(name);
        if (file == null) {
            log.info("No " + name + " file to backup");
        } else if (file.canWrite()) {
            String backupName = backupFileName(file.getAbsolutePath());
            File backupFile = findFile(backupName);
            if (backupFile != null) {
                if (backupFile.delete()) {
                    log.debug("deleted backup file " + backupName);
                }
            }
            if (file.renameTo(new File(backupName))) {
                log.debug("created new backup file " + backupName);
            } else {
                log.error("could not create backup file " + backupName);
            }
        }
    }

    /**
     * Move original file to backup directory.
     *
     * @param directory the backup directory to use.
     * @param file      the file to be backed up. The file name will have the
     *                  current date embedded in the backup name.
     * @return true if successful.
     */
    public boolean makeBackupFile(String directory, File file) {
        if (file == null) {
            log.info("No file to backup");
        } else if (file.canWrite()) {
            String backupFullName = directory + File.separator + createFileNameWithDate(file.getName());
            if (log.isDebugEnabled()) {
                log.debug("new backup file: " + backupFullName);
            }

            File backupFile = findFile(backupFullName);
            if (backupFile != null) {
                if (backupFile.delete()) {
                    if (log.isDebugEnabled()) {
                        log.debug("deleted backup file " + backupFullName);
                    }
                }
            } else {
                backupFile = new File(backupFullName);
            }
            // create directory if needed
            File parentDir = backupFile.getParentFile();
            if (!parentDir.exists()) {
                if (log.isDebugEnabled()) {
                    log.debug("creating backup directory: " + parentDir.getName());
                }
                if (!parentDir.mkdirs()) {
                    log.error("backup directory not created");
                    return false;
                }
            }
            if (file.renameTo(new File(backupFullName))) {
                if (log.isDebugEnabled()) {
                    log.debug("created new backup file " + backupFullName);
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("could not create backup file " + backupFullName);
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Revert to original file from backup. Use this for testing backup files.
     *
     * @param name Last part of file pathname i.e. subdir/name, without the
     *             pathname for either the xml or preferences directory.
     */
    public void revertBackupFile(String name) {
        File file = findFile(name);
        if (file == null) {
            log.info("No " + name + " file to revert");
        } else {
            String backupName = backupFileName(file.getAbsolutePath());
            File backupFile = findFile(backupName);
            if (backupFile != null) {
                log.info("No " + backupName + " backup file to revert");
                if (file.delete()) {
                    log.debug("deleted original file " + name);
                }

                if (backupFile.renameTo(new File(name))) {
                    log.debug("created original file " + name);
                } else {
                    log.error("could not create original file " + name);
                }
            }
        }
    }

    /**
     * Return the name of a new, unique backup file. This is here so it can be
     * overridden during tests. File to be backed-up must be within the
     * preferences directory tree.
     *
     * @param name Filename without preference path information, e.g.
     *             "decoders/Mine.xml".
     * @return Complete filename, including path information into preferences
     *         directory
     */
    public String backupFileName(String name) {
        String f = name + ".bak";
        if (log.isDebugEnabled()) {
            log.debug("backup file name is: " + f);
        }
        return f;
    }

    public String createFileNameWithDate(String name) {
        // remove .xml extension
        String[] fileName = name.split(".xml");
        String f = fileName[0] + "_" + getDate() + ".xml";
        if (log.isDebugEnabled()) {
            log.debug("backup file name is: " + f);
        }
        return f;
    }

    /**
     * @return String based on the current date in the format of year month day
     *         hour minute second. The date is fixed length and always returns a
     *         date represented by 14 characters.
     */
    private String getDate() {
        Calendar now = Calendar.getInstance();
        int month = now.get(Calendar.MONTH) + 1;
        String m = Integer.toString(month);
        if (month < 10) {
            m = "0" + Integer.toString(month);
        }
        int day = now.get(Calendar.DATE);
        String d = Integer.toString(day);
        if (day < 10) {
            d = "0" + Integer.toString(day);
        }
        int hour = now.get(Calendar.HOUR);
        String h = Integer.toString(hour);
        if (hour < 10) {
            h = "0" + Integer.toString(hour);
        }
        int minute = now.get(Calendar.MINUTE);
        String min = Integer.toString(minute);
        if (minute < 10) {
            min = "0" + Integer.toString(minute);
        }
        int second = now.get(Calendar.SECOND);
        String sec = Integer.toString(second);
        if (second < 10) {
            sec = "0" + Integer.toString(second);
        }
        String date = "" + now.get(Calendar.YEAR) + m + d + h + min + sec;
        return date;
    }

    /**
     * Execute the Processing Instructions in the file.
     *
     * JMRI only knows about certain ones; the others will be ignored.
     *
     * @return the Document that results from the processing
     */
    Document processInstructions(Document doc) {
        // this iterates over top level
        for (Object c : doc.getContent()) { // type Content
            if (c instanceof ProcessingInstruction) {
                try {
                    doc = processOneInstruction((ProcessingInstruction) c, doc);
                } catch (org.jdom2.transform.XSLTransformException ex) {
                    log.error("XSLT error while transforming with " + c + ", ignoring transform", ex);
                } catch (org.jdom2.JDOMException ex) {
                    log.error("JDOM error while transforming with " + c + ", ignoring transform", ex);
                } catch (java.io.IOException ex) {
                    log.error("IO error while transforming with " + c + ", ignoring transform", ex);
                }
            }
        }

        return doc;
    }

    Document processOneInstruction(ProcessingInstruction p, Document doc) throws org.jdom2.transform.XSLTransformException, org.jdom2.JDOMException, java.io.IOException {
        log.trace("handling ", p);

        // check target
        String target = p.getTarget();
        if (!target.equals("transform-xslt")) {
            return doc;
        }

        String href = p.getPseudoAttributeValue("href");
        // we expect this to start with http://jmri.org/ and refer to the JMRI file tree
        if (!href.startsWith("http://jmri.org/")) {
            return doc;
        }
        href = href.substring(16);

        // if starts with 'xml/' we remove that; findFile will put it back
        if (href.startsWith("xml/")) {
            href = href.substring(4);
        }

        // read the XSLT transform into a Document to get XInclude done
        SAXBuilder builder = getBuilder(false);  // argument controls validation
        Document xdoc = builder.build(new BufferedInputStream(new FileInputStream(findFile(href))));
        org.jdom2.transform.XSLTransformer transformer = new org.jdom2.transform.XSLTransformer(xdoc);
        return transformer.transform(doc);
    }

    /**
     * Create the Document object to store a particular root Element.
     *
     * @param root Root element of the final document
     * @param dtd  name of an external DTD
     * @return new Document, with root installed
     */
    static public Document newDocument(Element root, String dtd) {
        Document doc = new Document(root);
        doc.setDocType(new DocType(root.getName(), dtd));
        addDefaultInfo(root);
        return doc;
    }

    /**
     * Create the Document object to store a particular root Element, without a
     * DocType DTD (e.g. for using a schema)
     *
     * @param root Root element of the final document
     * @return new Document, with root installed
     */
    static public Document newDocument(Element root) {
        Document doc = new Document(root);
        addDefaultInfo(root);
        return doc;
    }

    /**
     * Add default information to the XML before writing it out.
     * <P>
     * Currently, this is identification information as an XML comment. This
     * includes: <UL>
     * <LI>The JMRI version used <LI>Date of writing <LI>A CVS id string, in
     * case the file gets checked in or out </UL>
     * <P>
     * It may be necessary to extend this to check whether the info is already
     * present, e.g. if re-writing a file.
     *
     * @param root The root element of the document that will be written.
     */
    static public void addDefaultInfo(Element root) {
        String content = "Written by JMRI version " + jmri.Version.name()
                + " on " + (new Date()).toString()
                + " $Id$";
        Comment comment = new Comment(content);
        root.addContent(comment);
    }

    /**
     * Define the location of XML files within the distribution directory.
     * <p>
     * Use {@link FileUtil#getProgramPath()} since the current working directory
     * is not guaranteed to be the JMRI distribution directory if jmri.jar is
     * referenced by an external Java application.
     *
     * @return the XML directory that ships with JMRI.
     */
    static public String xmlDir() {
        return FileUtil.getProgramPath() + "xml" + File.separator;
    }
    static boolean verify = false;
    static boolean include = true;

    static public boolean getVerify() {
        return verify;
    }

    static public void setVerify(boolean v) {
        verify = v;
    }

    /**
     * Provide a JFileChooser initialized to the default user location, and with
     * a default filter.
     *
     * @param filter  Title for the filter, may not be null
     * @param suffix1 An allowed suffix, or null
     * @param suffix2 A second allowed suffix, or null. If both arguments are
     *                null, no specific filtering is done.
     * @return a file chooser
     */
    public static JFileChooser userFileChooser(
            String filter, String suffix1, String suffix2) {
        JFileChooser fc = new JFileChooser(FileUtil.getUserFilesPath());
        NoArchiveFileFilter filt = new NoArchiveFileFilter(filter);
        if (suffix1 != null) {
            filt.addExtension(suffix1);
        }
        if (suffix2 != null) {
            filt.addExtension(suffix2);
        }
        fc.setFileFilter(filt);
        return fc;
    }

    public static JFileChooser userFileChooser() {
        JFileChooser fc = new JFileChooser(FileUtil.getUserFilesPath());
        NoArchiveFileFilter filt = new NoArchiveFileFilter();
        fc.setFileFilter(filt);
        return fc;
    }

    public static JFileChooser userFileChooser(String filter) {
        return userFileChooser(filter, null, null);
    }

    public static JFileChooser userFileChooser(
            String filter, String suffix1) {
        return userFileChooser(filter, suffix1, null);
    }

    public static SAXBuilder getBuilder(boolean verify) {
        SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser", verify);  // argument controls validation

        builder.setEntityResolver(new JmriLocalEntityResolver());
        builder.setFeature("http://apache.org/xml/features/xinclude", true);
        builder.setFeature("http://apache.org/xml/features/xinclude/fixup-base-uris", false);
        builder.setFeature("http://apache.org/xml/features/allow-java-encodings", true);

        // for schema validation. Not needed for DTDs, so continue if not found now
        try {
            builder.setFeature("http://apache.org/xml/features/validation/schema", verify);
            builder.setFeature("http://apache.org/xml/features/validation/schema-full-checking", verify);

            // parse namespaces, including the noNamespaceSchema
            builder.setFeature("http://xml.org/sax/features/namespaces", true);

        } catch (Exception e) {
            log.warn("Could not set schema validation feature: " + e);
        }
        return builder;
    }
    // initialize logging
    private static final Logger log = LoggerFactory.getLogger(XmlFile.class.getName());
}
