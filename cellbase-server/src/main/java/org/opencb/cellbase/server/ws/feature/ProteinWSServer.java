/*
 * Copyright 2015 OpenCB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencb.cellbase.server.ws.feature;

import com.google.common.base.Splitter;
import io.swagger.annotations.*;
import org.opencb.biodata.formats.protein.uniprot.v201504jaxb.Entry;
import org.opencb.cellbase.core.api.ProteinDBAdaptor;
import org.opencb.cellbase.core.api.TranscriptDBAdaptor;
import org.opencb.cellbase.server.exception.SpeciesException;
import org.opencb.cellbase.server.exception.VersionException;
import org.opencb.cellbase.server.ws.GenericRestWSServer;
import org.opencb.commons.datastore.core.Query;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.commons.datastore.core.QueryResult;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Path("/{version}/{species}/feature/protein")
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Protein", description = "Protein RESTful Web Services API")
public class ProteinWSServer extends GenericRestWSServer {

    public ProteinWSServer(@PathParam("version")
                           @ApiParam(name = "version", value = "Possible values: v3, v4",
                                   defaultValue = "v4") String version,
                           @PathParam("species")
                           @ApiParam(name = "species", value = "Name of the species, e.g.: hsapiens. For a full list "
                                   + "of potentially available species ids, please refer to: "
                                   + "http://bioinfo.hpc.cam.ac.uk/cellbase/webservices/rest/v4/meta/species") String species,
                           @Context UriInfo uriInfo,
                           @Context HttpServletRequest hsr) throws VersionException, SpeciesException, IOException {
        super(version, species, uriInfo, hsr);
    }

    @GET
    @Path("/model")
    @ApiOperation(httpMethod = "GET", value = "Returns a JSON specification of the protein data model",
            response = Map.class, responseContainer = "QueryResponse")
    public Response getModel() {
        return createModelResponse(Entry.class);
    }

    @GET
    @Path("/{proteinId}/info")
    @ApiOperation(httpMethod = "GET", value = "Get the protein info", response = Entry.class,
            responseContainer = "QueryResponse")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "keyword",
                    value = "Comma separated list of keywords that may be associated with the protein(s), e.g.: "
                            + "Transcription,Zinc. Exact text matches will be returned",
                    required = false, dataType = "java.util.List", paramType = "query")
    })
    public Response getInfoByEnsemblId(@PathParam("proteinId")
                                       @ApiParam(name = "proteinId",
                                               value = "Comma separated list of xrefs ids, e.g.: CCDS31418.1,Q9UL59,"
                                                       + " ENST00000278314. Exact text matches will be returned",
                                               required = true) String id) {
        try {
            parseQueryParams();
            ProteinDBAdaptor proteinDBAdaptor = dbAdaptorFactory.getProteinDBAdaptor(this.species, this.assembly);
            List<Query> queries = createQueries(id, ProteinDBAdaptor.QueryParams.XREFS.key());
            List<QueryResult> queryResults = proteinDBAdaptor.nativeGet(queries, queryOptions);
            for (int i = 0; i < queries.size(); i++) {
                queryResults.get(i).setId((String) queries.get(i).get(ProteinDBAdaptor.QueryParams.XREFS.key()));
            }
            return createOkResponse(queryResults);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/search")
    @ApiOperation(httpMethod = "GET", notes = "No more than 1000 objects are allowed to be returned at a time.",
            value = "Get all proteins", response = Entry.class, responseContainer = "QueryResponse")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "accession",
                    value = "Comma separated list of UniProt accession ids, e.g.: Q9UL59,B2R8Q1,Q9UKT9."
                            + "Exact text matches will be returned",
                    required = false, dataType = "java.util.List", paramType = "query"),
            @ApiImplicitParam(name = "name",
                    value = "Comma separated list of protein names, e.g.: ZN214_HUMAN,MKS1_HUMAN"
                            + "Exact text matches will be returned",
                    required = false, dataType = "java.util.List", paramType = "query"),
            @ApiImplicitParam(name = "gene",
                    value = "Comma separated list gene ids, e.g.: BRCA2."
                            + "Exact text matches will be returned",
                    required = false, dataType = "java.util.List", paramType = "query"),
            @ApiImplicitParam(name = "xrefs",
                    value = "Comma separated list of xrefs ids, e.g.: CCDS31418.1,Q9UL59,ENST00000278314"
                            + "Exact text matches will be returned",
                    required = false, dataType = "java.util.List", paramType = "query"),
            @ApiImplicitParam(name = "keyword",
                    value = "Comma separated list of keywords that may be associated with the protein(s), e.g.: "
                            + "Transcription,Zinc. Exact text matches will be returned",
                    required = false, dataType = "java.util.List", paramType = "query")
    })
    public Response getAll() {
        try {
            parseQueryParams();
            ProteinDBAdaptor proteinDBAdaptor = dbAdaptorFactory.getProteinDBAdaptor(this.species, this.assembly);
            return createOkResponse(proteinDBAdaptor.nativeGet(query, queryOptions));
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/{proteinId}/substitution_scores")
    @ApiOperation(httpMethod = "GET", value = "Get the gene corresponding substitution scores for the input protein",
        notes = "Schema of returned objects will vary depending on provided query parameters. If the amino acid "
                + " position is provided, all scores will be returned for every possible amino acid"
                + " change occurring at that position. If the alternate aminoacid is provided as well, Score objects as"
                + " specified at "
                + " https://github.com/opencb/biodata/blob/develop/biodata-models/src/main/resources/avro/variantAnnotation.avdl"
                + " shall be returned. If none of these parameters are provided, the whole list of scores for every"
                + " possible amino acid change in the protein shall be returned.",
            response = List.class, responseContainer = "QueryResponse")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "position",
                    value = "Integer indicating the aminoacid position to check",
                    required = false, dataType = "Integer", paramType = "query"),
            @ApiImplicitParam(name = "aa",
                    value = "Alternate aminoacid to check. Please, use upper-case letters and three letter encoding"
                            + " of aminoacid names, e.g.: CYS",
                    required = false, dataType = "String", paramType = "query")
    })
    public Response getSubstitutionScores(@PathParam("proteinId")
                                          @ApiParam(name = "proteinId",
                                                  value = "String indicating one xref id, e.g.: Q9UL59, Exact text "
                                                          + "matches will be returned",
                                                  required = true) String id) {
        try {
            parseQueryParams();
//            query.put(ProteinDBAdaptor.QueryParams.XREFS.key(), id);

            // Fetch Ensembl transcriptId to query substiturion scores
            TranscriptDBAdaptor transcriptDBAdaptor = dbAdaptorFactory.getTranscriptDBAdaptor(this.species, this.assembly);
            logger.info("Searching transcripts for protein {}", id);
            Query transcriptQuery = new Query(TranscriptDBAdaptor.QueryParams.XREFS.key(), id);
            QueryOptions transcriptQueryOptions = new QueryOptions("include", "transcripts.id");
            QueryResult queryResult = transcriptDBAdaptor.nativeGet(transcriptQuery, transcriptQueryOptions);
            logger.info("{} transcripts found", queryResult.getNumResults());
            logger.info("Transcript IDs: {}", jsonObjectWriter.writeValueAsString(queryResult.getResult()));

            // Get substitution scores for fetched transcript
            if (queryResult.getNumResults() > 0) {
                query.put("transcript", ((Map) queryResult.getResult().get(0)).get("id"));
                logger.info("Getting substitution scores for query {}", jsonObjectWriter.writeValueAsString(query));
                logger.info("queryOptions {}", jsonObjectWriter.writeValueAsString(queryOptions));
                ProteinDBAdaptor proteinDBAdaptor = dbAdaptorFactory.getProteinDBAdaptor(this.species, this.assembly);
                QueryResult scoresQueryResult = proteinDBAdaptor.getSubstitutionScores(query, queryOptions);
                scoresQueryResult.setId(id);
                return createOkResponse(scoresQueryResult);
            } else {
                return createOkResponse(queryResult);
            }
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/{proteinId}/name")
    @ApiOperation(httpMethod = "GET", value = "Deprecated", hidden = true)
    @Deprecated
    public Response getproteinByName(@PathParam("proteinId") String id) {
        try {
            parseQueryParams();
            ProteinDBAdaptor geneDBAdaptor = dbAdaptorFactory.getProteinDBAdaptor(this.species, this.assembly);
            return createOkResponse(geneDBAdaptor.get(Splitter.on(",").splitToList(id), queryOptions));
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/{proteinId}/gene")
    @ApiOperation(httpMethod = "GET", value = "Get the gene corresponding to the input protein", hidden = true)
    public Response getGene(@PathParam("proteinId") String query) {
        return null;
    }

    @GET
    @Path("/{proteinId}/transcript")
    @ApiOperation(httpMethod = "GET", value = "To be implemented", hidden = true)
    public Response getTranscript(@PathParam("proteinId") String query) {
        return null;
    }

//  @GET
//    @Path("/{proteinId}/feature")
//    public Response getFeatures(@PathParam("proteinId") String query, @DefaultValue("") @QueryParam("type") String type) {
//        try {
//            parseQueryParams();
//            ProteinDBAdaptor adaptor = dbAdaptorFactory.getProteinDBAdaptor(this.species, this.assembly);
//            return generateResponse(query, "PROTEIN_FEATURE",
// adaptor.getAllProteinFeaturesByProteinXrefList(Splitter.on(",").splitToList(query)));
//        } catch (Exception e) {
//            e.printStackTrace();
//            return createErrorResponse("getFeatures", e.toString());
//        }
//    }

//    @GET
//    @Path("/{proteinName}/function_prediction")
//    public Response getFunctionalPredictions(@PathParam("proteinName") String query,
// @DefaultValue("") @QueryParam("source") String source) {
//        try {
//            parseQueryParams();
//            queryOptions.put("disease", Splitter.on(",").splitToList(source));
//            ProteinDBAdaptor adaptor = dbAdaptorFactory.getProteinDBAdaptor(this.species, this.assembly);
//            return generateResponse(query, "PROTEIN_FEATURE",
// adaptor.getAllProteinFeaturesByProteinXrefList(Splitter.on(",").splitToList(query)));
//        } catch (Exception e) {
//            e.printStackTrace();
//            return createErrorResponse("getFeatures", e.toString());
//        }
//    }

//    @GET
//    @Path("/{proteinId}/association")
//    public Response getInteraction(@PathParam("proteinId") String query, @DefaultValue("") @QueryParam("type") String type) {
//        return null;
//    }

//    @GET
//    @Path("/{proteinId}/xref")
//    public Response getXrefs(@PathParam("proteinId") String proteinId, @DefaultValue("") @QueryParam("dbname") String dbname) {
//        try {
//            parseQueryParams();
//            ProteinDBAdaptor adaptor = dbAdaptorFactory.getProteinDBAdaptor(this.species, this.assembly);
//            return generateResponse(proteinId, "XREFS",
// adaptor.getAllProteinXrefsByProteinNameList(Splitter.on(",").splitToList(proteinId)));
//        } catch (Exception e) {
//            e.printStackTrace();
//            return createErrorResponse("getXrefs", e.toString());
//        }
//    }

    @Deprecated
    @GET
    @Path("/{proteinId}/reference")
    @ApiOperation(httpMethod = "GET", value = "Deprecated", hidden = true)
    public Response getReference(@PathParam("proteinId") String query) {
        return null;
    }

//    @GET
//    @Path("/{proteinId}/interaction")
//    public Response getInteraction(@PathParam("proteinId") String query, @DefaultValue("") @QueryParam("source") String source) {
//        try {
//            parseQueryParams();
//            ProteinDBAdaptor adaptor = dbAdaptorFactory.getProteinDBAdaptor(this.species, this.assembly);
//            if(source != null && !source.equals("")) {
//                return generateResponse(query, "PROTEIN_INTERACTION",
// adaptor.getAllProteinInteractionsByProteinNameList(Splitter.on(",").splitToList(query), source));
//            }else{
//                return generateResponse(query, "PROTEIN_INTERACTION",
// adaptor.getAllProteinInteractionsByProteinNameList(Splitter.on(",").splitToList(query)));
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            return createErrorResponse("getInteraction", e.toString());
//        }
//    }

    @GET
    @Path("/{proteinId}/sequence")
    @ApiOperation(httpMethod = "GET", value = "Get the aa sequence for the given protein", response = String.class,
        responseContainer = "QueryResponse")
    public Response getSequence(@PathParam("proteinId")
                                @ApiParam (name = "proteinId", value = "UniProt accession id, e.g: Q9UL59",
                                        required = true) String proteinId) {
        ProteinDBAdaptor proteinDBAdaptor = dbAdaptorFactory.getProteinDBAdaptor(this.species, this.assembly);
        query.put(ProteinDBAdaptor.QueryParams.ACCESSION.key(), proteinId);
        queryOptions.put("include", "sequence.value");
        // split by comma
        QueryResult<Entry> queryResult = proteinDBAdaptor.get(query, queryOptions);
//        Document sequenceDocument = (Document) ((Document)queryResult.first()).get("sequence");
////        String sequence = sequenceDocument.getString("value");
//        queryResult.setResult(Collections.singletonList(sequenceDocument.getString("value")));
        QueryResult queryResult1 = new QueryResult(queryResult.getId(), queryResult.getDbTime(), queryResult.getNumResults(),
                queryResult.getNumTotalResults(), queryResult.getWarningMsg(), queryResult.getErrorMsg(), Collections.EMPTY_LIST);
        queryResult1.setResult(Collections.singletonList(queryResult.first().getSequence().getValue()));
        queryResult1.setId(proteinId);
        return createOkResponse(queryResult1);
    }

    @GET
    public Response defaultMethod() {
        return help();
    }

    @GET
    @Path("/help")
    public Response help() {
        StringBuilder sb = new StringBuilder();
        sb.append("Input:\n");
        sb.append("all id formats are accepted.\n\n\n");
        sb.append("Resources:\n");
        sb.append("- info: Get protein information: name, UniProt ID and description.\n");
        sb.append(" Output columns: UniProt accession, protein name, full name, gene name, organism.\n\n");
        sb.append("- feature: Get particular features for the protein sequence: natural variants in the aminoacid sequence, "
                + "mutagenesis sites, etc.\n");
        sb.append(" Output columns: feature type, aa start, aa end, original, variation, identifier, description.\n\n\n");
        sb.append("Documentation:\n");
        sb.append("http://docs.bioinfo.cipf.es/projects/cellbase/wiki/Feature_rest_ws_api#Protein");

        return createOkResponse(sb.toString());
    }

}
