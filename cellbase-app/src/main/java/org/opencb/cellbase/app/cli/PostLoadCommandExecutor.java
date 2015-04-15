package org.opencb.cellbase.app.cli;

import com.beust.jcommander.ParameterException;
import org.opencb.biodata.formats.annotation.io.VepFormatReader;
import org.opencb.biodata.models.variant.annotation.VariantAnnotation;
import org.opencb.cellbase.core.CellBaseConfiguration;
import org.opencb.cellbase.core.client.CellBaseClient;
import org.opencb.cellbase.core.lib.DBAdaptorFactory;
import org.opencb.cellbase.core.lib.api.variation.ClinicalDBAdaptor;
import org.opencb.cellbase.core.variant_annotation.VariantAnnotatorRunner;
import org.opencb.cellbase.mongodb.db.MongoDBAdaptorFactory;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by fjlopez on 14/04/15.
 */
public class PostLoadCommandExecutor extends CommandExecutor{

    private CliOptionsParser.PostLoadCommandOptions postLoadCommandOptions;

    private Path clinicalAnnotationFilename = null;

    public PostLoadCommandExecutor(CliOptionsParser.PostLoadCommandOptions postLoadCommandOptions) {
        super(postLoadCommandOptions.commonOptions.logLevel, postLoadCommandOptions.commonOptions.verbose,
                postLoadCommandOptions.commonOptions.conf);

        this.postLoadCommandOptions = postLoadCommandOptions;
    }

    @Override
    public void execute() {
        checkParameters();
        if(clinicalAnnotationFilename!=null) {
            loadClinicalAnnotation();
        } else {
            throw new ParameterException("Only post-load of clinical annotations is available right now.");
        }
    }

    private void checkParameters() {
        // input file
        if (postLoadCommandOptions.clinicalAnnotationFilename != null) {
            clinicalAnnotationFilename = Paths.get(postLoadCommandOptions.clinicalAnnotationFilename);
            if (!clinicalAnnotationFilename.toFile().exists()) {
                throw new ParameterException("Input file " + clinicalAnnotationFilename + " doesn't exist");
            } else if (clinicalAnnotationFilename.toFile().isDirectory()) {
                throw new ParameterException("Input file cannot be a directory: " + clinicalAnnotationFilename);
            }
        } else {
            throw  new ParameterException("Please check command line syntax. Provide a valid input file name.");
        }
    }

    private void loadClinicalAnnotation() {

        VepFormatReader vepFormatReader = new VepFormatReader(clinicalAnnotationFilename.toString());


        org.opencb.cellbase.core.common.core.CellbaseConfiguration adaptorCellbaseConfiguration =
                new org.opencb.cellbase.core.common.core.CellbaseConfiguration();
        adaptorCellbaseConfiguration.addSpeciesAlias("hsapiens", "hsapiens");
//        config.addSpeciesConnection("hsapiens", "GRCh37", "mongodb-hxvm-var-001", "cellbase_hsapiens_grch37_v3", 27017,
//                "mongo", "biouser", "B10p@ss", 10, 10);

        configuration.getSpecies()
        DBAdaptorFactory dbAdaptorFactory = new MongoDBAdaptorFactory();
        ClinicalDBAdaptor clinicalDBAdaptor =
        List<VariantAnnotation> variantAnnotationList;

        while((variantAnnotationList=vepFormatReader.read(CLINICAL_ANNOTATION_BATCH_SIZE))!=null) {

        }
    }


}
