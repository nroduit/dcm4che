package org.dcm4che3.tool.dcmconv;

import java.nio.file.FileSystems;
import java.util.concurrent.TimeUnit;
import org.dcm4che3.data.Implementation;
import org.dcm4che3.data.UID;
import org.dcm4che3.imageio.codec.TransferSyntaxType;
import org.dcm4che3.img.DicomImageReadParam;
import org.dcm4che3.img.DicomTranscodeParam;
import org.dcm4che3.img.Transcoder;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

/**
 * @author Nicolas Roduit
 * @since Nov 2020
 */
@CommandLine.Command(
    name = "dcmconv",
    mixinStandardHelpOptions = true,
    versionProvider = Dcmconv.ModuleVersionProvider.class,
    descriptionHeading = "%n",
    description = "The dcmconv utility allows converting DICOM files with a specific transfer syntax.",
    parameterListHeading = "%nParameters:%n",
    optionListHeading = "%nOptions:%n",
    showDefaultValues = true,
    footerHeading = "%nExample:%n",
    footer = {"$ dcm2dcm -t JPEG_2000 -o \"/tmp/dcm/\" \"home/user/image.dcm\" \"home/user/dicom/\"",
        "Transcode DICOM file and folder into jpeg2000 syntax and write them into \"/tmp/dcm/\"." }
)
public class Dcmconv implements Callable<Integer> {

    public enum TransferSyntax {
        RAW_EXPLICIT_LE ("Explicit VR Little Endian", UID.ExplicitVRLittleEndian),
        JPEG_BASELINE_8 ("JPEG Baseline (Process 1)", UID.JPEGBaseline8Bit),
        JPEG_EXTENDED_12 ("JPEG Extended (Process 2 & 4),)", UID.JPEGExtended12Bit),
        JPEG_SPECTRAL ("JPEG Spectral Selection, Non-Hierarchical (Process 6 & 8) (Retired)", UID.JPEGSpectralSelectionNonHierarchical68),
        JPEG_PROGRESSIVE ("JPEG Full Progression, Non-Hierarchical (Process 10 & 12) (Retired)", UID.JPEGFullProgressionNonHierarchical1012),
        JPEG_LOSSLESS_P14 ("JPEG Lossless, Non-Hierarchical (Process 14)", UID.JPEGLossless),
        JPEG_LOSSLESS_SV1 ("JPEG Lossless, Non-Hierarchical, First-Order Prediction (Process 14 [Selection Value 1])", UID.JPEGLosslessSV1),
        JPEG_LS_LOSSLESS ("JPEG-LS Lossless Image Compression", UID.JPEGLSLossless),
        JPEG_LS_NEAR_LOSSLESS ("JPEG-LS Lossy (Near-Lossless) Image Compression", UID.JPEGLSNearLossless),
        JPEG_2000_LOSSLESS ("JPEG 2000 Image Compression (Lossless Only)", UID.JPEG2000Lossless),
        JPEG_2000 ("JPEG 2000 Image Compression,", UID.JPEG2000);

        private final String name;
        private final String uid;

        TransferSyntax(String name, String uid){
            this.name = name;
            this.uid = uid;
        }
    }

    @CommandLine.Parameters(
        description = "List of paths which can be DICOM file or folder.",
        arity = "1..*",
        index = "0")
    Path[] paths;

    @CommandLine.Option(
        names = {"-o", "--output"},
        required = true,
        description = "Path of the output image. if the path is a directory then the filename is taken from the source path.")
    Path outDir;

    @CommandLine.Option(names = "-t", description = "Transfer syntax: ${COMPLETION-CANDIDATES}")
    TransferSyntax syntax = TransferSyntax.RAW_EXPLICIT_LE;

  @CommandLine.Option(
      names = "-N",
      description = "Near-Lossless parameter of JPEG-LS Lossy compression (0 to n).")
  Integer nearLosslessError = 3;

    @CommandLine.Option(names = "-q", description = "Lossy JPEG compression quality between 1 to 100 (100 is the best lossy quality).")
    Integer jpegCompressionQuality = 80;

    @CommandLine.Option(names = "-Q", description = "Lossy JPEG2000 compression factor between 5 to 100 (5 is near lossless).")
    Integer compressionRatiofactor = 10;

    @CommandLine.Option(names = "--rgb-lossy", negatable = true, description = "Keep RGB model with JPEG lossy." +
        "If FALSE the reader force using YBR color model")
    boolean keepRgb = false;

    public static void main(String[] args) {
        CommandLine cl = new CommandLine(new Dcmconv());
        cl.execute(args);
    }

    @Override
    public Integer call() throws Exception {
        DicomImageReadParam readParam = new DicomImageReadParam();
        readParam.setKeepRgbForLossyJpeg(keepRgb);
        DicomTranscodeParam params = new DicomTranscodeParam(readParam, syntax.uid);
        if (params.getWriteJpegParam() != null && TransferSyntaxType.isLossyCompression(syntax.uid)) {
            params.getWriteJpegParam().setCompressionQuality(jpegCompressionQuality);
            params.getWriteJpegParam().setCompressionRatiofactor(compressionRatiofactor);
            params.getWriteJpegParam().setNearLosslessError(nearLosslessError);
        }

        System.out.printf("Converting all the images to %s [%s].%n", syntax.name, syntax.uid);
        long startTime = System.nanoTime();
        for (Path p: paths) {
            if(Files.isDirectory(p)) {
                try (Stream<Path> walk = Files.walk(p)) {
                    walk.forEach( path -> {
                        try {
                            Path out = FileSystems.getDefault().getPath(outDir.toString(), p.relativize(path).toString());
                            if(Files.isRegularFile(path)) {
                                Transcoder.dcm2dcm(path, out, params);
                                System.out.printf("Transcode \"%s\" in \"%s\".%n", path, out);
                            }
                            else {
                                Files.createDirectories(out);
                            }
                        } catch (Exception e) {
                            System.out.printf("Cannot convert \"%s\".%n", path);
                        }
                    });
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
            else{
                Transcoder.dcm2dcm(p, outDir, params);
                System.out.printf("Transcode \"%s\" in \"%s\".%n", p, outDir);
            }
        }
        long elapsedTime = System.nanoTime() - startTime;
        if(elapsedTime < 10_000_000_000L) {
            long ms =  TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);
            System.out.println("Convert in" + ms + " milliseconds");
        }
        else {
            long sec = TimeUnit.SECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);
            System.out.println(sec + " seconds");
        }
        return 0;
    }

    static class ModuleVersionProvider implements CommandLine.IVersionProvider {
        public String[] getVersion() {
            return new String[]{Implementation.getVersionName()};
        }
    }
}
