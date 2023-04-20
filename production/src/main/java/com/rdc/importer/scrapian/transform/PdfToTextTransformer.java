package com.rdc.importer.scrapian.transform;

import com.rdc.importer.misc.StreamUtils;
import com.rdc.importer.scrapian.model.ScrapianSource;
import com.rdc.importer.scrapian.model.StringSource;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class PdfToTextTransformer implements ScrapianTransformer {

    private static Logger logger = LogManager.getLogger(PdfToTextTransformer.class);

    private static File pdftotextExe;
    private static File runningDirectory;
    private static String osName;
    private String encoding = null;

    static {
        initExeExtraction();
    }

    private static void initExeExtraction() {
        String workDir = System.getProperty("com.rdc.importer.scrapian.PdfWorkDir");
        if (workDir == null) {
            workDir = System.getProperty("java.io.tmpdir");
        }

        String executableName;
        String osPath;

        osName = System.getProperty("os.name");
        if (osName.equals("Mac OS X")) {
            osPath = "/pdftotext/osx/";
            executableName = "pdftotext";
        } else if (osName.startsWith("Windows")) {
            osPath = "/pdftotext/win32/";
            executableName = "pdftotext.exe";
        } else if (osName.equals("Linux")) {
            osPath = "/pdftotext/linux64/";
            executableName = "pdftotext";
        } else {
            throw new RuntimeException("Unknown operating system [" + osName + "]");
        }

        runningDirectory = new File(workDir);
        pdftotextExe = new File(runningDirectory, executableName);
        if (!pdftotextExe.exists()) {
            logger.info("Creating[" + pdftotextExe.getAbsolutePath() + "]");
            try {
                FileUtils.copyURLToFile(PdfToTextTransformer.class.getResource(osPath + executableName), pdftotextExe);
                //FileUtils.copyFileToDirectory(new File("C:/rdc_home/projects/java/list_importer/trunk/src/main/resources"+osPath + executableName), runningDirectory);
                pdftotextExe.setExecutable(true);
            } catch (IOException e) {
                throw new RuntimeException("Unable to create[" + pdftotextExe.getAbsolutePath() + "]", e);
            }
        }
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public ScrapianSource transform(ScrapianSource scrapianSource, Map<String, Object> parameters) throws Exception {

        File inputFile = File.createTempFile("p2t", ".pdf", runningDirectory);
        File outputFile = File.createTempFile("p2t", ".txt", runningDirectory);
        try {
            FileUtils.writeByteArrayToFile(inputFile, (byte[]) scrapianSource.getValue());
            if (parameters == null) {
                //executeCommand(runningDirectory, pdftotextExe.getAbsolutePath(), "-layout", inputFile.getAbsolutePath(), outputFile.getAbsolutePath());
								//Use for Windows
								 executeCommand(runningDirectory, pdftotextExe.getName(), "-layout", inputFile.getAbsolutePath(), outputFile.getAbsolutePath());

            } else {
                int valsz = parameters.values().size();
                String[] params = parameters.values().toArray(new String[valsz + 2]);
                params[valsz++] = inputFile.getAbsolutePath();
                params[valsz] = outputFile.getAbsolutePath();
                /*Uses For Ubuntu*/

               // executeCommand(runningDirectory, pdftotextExe.getAbsolutePath(), params);
                /*Uses for windows*/
                 executeCommand(runningDirectory, pdftotextExe.getName(), params);
            }
            if (encoding == null)
                return new StringSource(FileUtils.readFileToString(outputFile));
            else
                return new StringSource(FileUtils.readFileToString(outputFile, encoding));
        } finally {
            inputFile.delete();
            outputFile.delete();
        }
    }

    private String executeCommand(File directory, String command, String... args) throws Exception {
        List<String> commandList = new ArrayList<String>();
        if (osName.startsWith("Windows")) {
            commandList.add(System.getenv("windir") + "\\system32\\" + "cmd.exe");
            commandList.add("/c");
        }
        commandList.add(command);
        commandList.addAll(Arrays.asList(args));
        ProcessBuilder pb = new ProcessBuilder(commandList);
        if (directory != null) {
            pb.directory(directory);
        }
        Process p = pb.start();

        StreamReaderThread inputStreamReaderThread = new StreamReaderThread(p.getInputStream());
        StreamReaderThread errorStreamReaderThread = new StreamReaderThread(p.getErrorStream());
        inputStreamReaderThread.start();
        errorStreamReaderThread.start();
        int exitValue = p.waitFor();

        if (inputStreamReaderThread.exception != null) {
            throw inputStreamReaderThread.exception;
        }

        if (errorStreamReaderThread.exception != null) {
            throw errorStreamReaderThread.exception;
        }

        if (exitValue != 0) {
            throw new Exception("Program [" + command + "] abnormally exited with value=[" + exitValue + "] and error=[" + errorStreamReaderThread.content + "]");
        }
        return inputStreamReaderThread.content.toString();
    }

    static class StreamReaderThread implements Runnable {
        private InputStream is;
        private Exception exception;
        private StringBuffer content = new StringBuffer();

        public StreamReaderThread(InputStream is) {
            this.is = is;
        }

        public void start() {
            Thread thread = new Thread(this);
            thread.start();
        }

        public void run() {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line = br.readLine();
                while (line != null) {
                    if (content.length() != 0) {
                        content.append("\n");
                    }
                    content.append(line);
                    line = br.readLine();
                }
            } catch (Throwable e) {
                exception = new Exception("Error reading stream", e);
            } finally {
                StreamUtils.closeQuietly(is);
            }
        }
    }


}
