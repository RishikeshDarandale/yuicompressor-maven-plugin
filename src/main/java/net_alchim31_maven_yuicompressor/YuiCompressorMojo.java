package net_alchim31_maven_yuicompressor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.zip.GZIPOutputStream;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

import com.yahoo.platform.yui.compressor.CssCompressor;

/**
 * Apply compression on JS and CSS (using YUI Compressor).
 *
 * @author David Bernard
 * @created 2007-08-28
 * @threadSafe
 */
// @SuppressWarnings("unchecked")
public abstract class YuiCompressorMojo extends MojoSupport {

    /**
     * Read the input file using "encoding".
     *
     * @parameter expression="${file.encoding}" default-value="UTF-8"
     */
    private String encoding;

    /**
     * The output filename suffix.
     *
     * @parameter expression="${maven.yuicompressor.suffix}" default-value="-min"
     */
    private String suffix;

    /**
     * If no "suffix" must be add to output filename (maven's configuration manage empty suffix like default).
     *
     * @parameter expression="${maven.yuicompressor.nosuffix}" default-value="false"
     */
    private boolean nosuffix;

    /**
     * Insert line breaks in output after the specified column number.
     *
     * @parameter expression="${maven.yuicompressor.linebreakpos}" default-value="-1"
     */
    private int linebreakpos;

    /**
     * [js only] No compression
     *
     * @parameter expression="${maven.yuicompressor.nocompress}" default-value="false"
     */
    private boolean nocompress;

    /**
     * [js only] Minify only, do not obfuscate.
     *
     * @parameter expression="${maven.yuicompressor.nomunge}" default-value="false"
     */
    private boolean nomunge;

    /**
     * [js only] Preserve unnecessary semicolons.
     *
     * @parameter expression="${maven.yuicompressor.preserveAllSemiColons}" default-value="false"
     */
    private boolean preserveAllSemiColons;

    /**
     * [js only] disable all micro optimizations.
     *
     * @parameter expression="${maven.yuicompressor.disableOptimizations}" default-value="false"
     */
    private boolean disableOptimizations;

    /**
     * force the compression of every files,
     * else if compressed file already exists and is younger than source file, nothing is done.
     *
     * @parameter expression="${maven.yuicompressor.force}" default-value="false"
     */
    private boolean force;

    /**
     * a list of aggregation/concatenation to do after processing,
     * for example to create big js files that contain several small js files.
     * Aggregation could be done on any type of file (js, css, ...).
     *
     * @parameter
     */
    private Aggregation[] aggregations;

    /**
     * request to create a gzipped version of the yuicompressed/aggregation files.
     *
     * @parameter expression="${maven.yuicompressor.gzip}" default-value="false"
     */
    private boolean gzip;

    /**
     * show statistics (compression ratio).
     *
     * @parameter expression="${maven.yuicompressor.statistics}" default-value="true"
     */
    private boolean statistics;

    /**
     * aggregate files before minify
     * @parameter expression="${maven.yuicompressor.preProcessAggregates}" default-value="false"
     */
    private boolean preProcessAggregates;

    protected long inSizeTotal_;
    protected long outSizeTotal_;

    @Override
    protected String[] getDefaultIncludes() throws Exception {
        return new String[]{"**/*.css", "**/*.js"};
    }

    @Override
    public void beforeProcess() throws Exception {
        if (nosuffix) {
            suffix = "";
        }

        if(preProcessAggregates) aggregate();
    }

    @Override
    protected void afterProcess() throws Exception {
        if (statistics && (inSizeTotal_ > 0)) {
            getLog().info(String.format("total input (%db) -> output (%db)[%d%%]", inSizeTotal_, outSizeTotal_, ((outSizeTotal_ * 100)/inSizeTotal_)));
        }

        if(!preProcessAggregates) aggregate();
    }

    private void aggregate() throws Exception {
        if (aggregations != null) {
            for(Aggregation aggregation : aggregations) {
                getLog().info("generate aggregation : " + aggregation.output);
                aggregation.run();
                File gzipped = gzipIfRequested(aggregation.output);
                if (statistics) {
                    if (gzipped != null) {
                        getLog().info(String.format("%s (%db) -> %s (%db)[%d%%]", aggregation.output.getName(), aggregation.output.length(), gzipped.getName(), gzipped.length(), ratioOfSize(aggregation.output, gzipped)));
                    } else if (aggregation.output.exists()){
                        getLog().info(String.format("%s (%db)", aggregation.output.getName(), aggregation.output.length()));
                    } else {
                        getLog().warn(String.format("%s not created", aggregation.output.getName()));
                    }
                }
            }
        }
    }
    
    protected abstract void processFile(SourceFile src) throws Exception;
    
	protected void compressCss(InputStreamReader in, OutputStreamWriter out)
			throws IOException {
		try{
		    CssCompressor compressor = new CssCompressor(in);
		    compressor.compress(out, linebreakpos);
		}catch(IllegalArgumentException e){
			throw new IllegalArgumentException(
					"Unexpected characters found in CSS file. Ensure that the CSS file does not contain '$', and try again",e);
		}
	}

    protected File gzipIfRequested(File file) throws Exception {
        if (!gzip || (file == null) || (!file.exists())) {
            return null;
        }
        if (".gz".equalsIgnoreCase(FileUtils.getExtension(file.getName()))) {
            return null;
        }
        File gzipped = new File(file.getAbsolutePath()+".gz");
        getLog().debug(String.format("create gzip version : %s", gzipped.getName()));
        GZIPOutputStream out = null;
        FileInputStream in = null;
        try {
            out = new GZIPOutputStream(new FileOutputStream(gzipped));
            in = new FileInputStream(file);
            IOUtil.copy(in, out);
        } finally {
            IOUtil.close(in);
            IOUtil.close(out);
        }
        return gzipped;
    }

    protected long ratioOfSize(File file100, File fileX) throws Exception {
        long v100 = Math.max(file100.length(), 1);
        long vX = Math.max(fileX.length(), 1);
        return (vX * 100)/v100;
    }

	public String getEncoding() {
		return encoding;
	}

	public String getSuffix() {
		return suffix;
	}

	public boolean isNosuffix() {
		return nosuffix;
	}

	public int getLinebreakpos() {
		return linebreakpos;
	}

	public boolean isNocompress() {
		return nocompress;
	}

	public boolean isNomunge() {
		return nomunge;
	}

	public boolean isPreserveAllSemiColons() {
		return preserveAllSemiColons;
	}

	public boolean isDisableOptimizations() {
		return disableOptimizations;
	}

	public boolean isForce() {
		return force;
	}

	public Aggregation[] getAggregations() {
		return aggregations;
	}

	public boolean isGzip() {
		return gzip;
	}

	public boolean isStatistics() {
		return statistics;
	}

	public boolean isPreProcessAggregates() {
		return preProcessAggregates;
	}

	public long getInSizeTotal_() {
		return inSizeTotal_;
	}

	public long getOutSizeTotal_() {
		return outSizeTotal_;
	}
}
