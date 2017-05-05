package com.asr.controller;

import com.ASR;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Created by marnie on 11/4/17.
 */
@Controller
@RequestMapping(value = "/asr")
class ASRController {

    final String sessionId = "grasp" + Long.toString(System.currentTimeMillis());

    final String sessionPath = "/home/ariane/Documents/bodenlab/data/WebSessions";

    //final String sessionPath = "/Users/marnie/Documents/WebSessions/";

    private ASR asr;

    /**
     * Get form.html template
     *
     * @return          form html template
     */
    @RequestMapping(method=RequestMethod.GET)
    public String showForm(Model model) {
        model.addAttribute("asrForm", new ASR());
        return "index";
    }

    /**
     * Submit the asr form (documenting input details, i.e. aln and tree file, etc)
     *
     * @param asrForm           ASR object
     * @param bindingResult     Form result, indicating any input errors
     * @param model             com model
     * @return                  index with results as attributes in the model
     */
    @RequestMapping(method=RequestMethod.POST, params="submit")
    public String performReconstruction(@Valid @ModelAttribute ASR asrForm, BindingResult bindingResult, Model model){
        System.out.println("Workin");
        if (bindingResult.hasErrors()) {
            for (String err : bindingResult.getSuppressedFields())
                System.out.println(err);
            return "index";
        }

        this.asr = asrForm;

        // upload supplied files
        try {
            File sessionDir = new File(sessionPath + sessionId);
            if (!sessionDir.exists())
                sessionDir.mkdir();

            asr.setSessionDir(sessionDir.getAbsolutePath() + "/");

            asr.getAlnFile().transferTo(new File(asr.getSessionDir() + asr.getAlnFile().getOriginalFilename()));
            asr.setAlnFilepath(asr.getSessionDir() + asr.getAlnFile().getOriginalFilename());
            asr.getTreeFile().transferTo(new File(asr.getSessionDir() + asr.getTreeFile().getOriginalFilename()));
            asr.setTreeFilepath(asr.getSessionDir() + asr.getTreeFile().getOriginalFilename());

            // TODO: push exceptions to error message on view...
            asr.runReconstruction();

            // add reconstructed newick string to send to javascript
            model.addAttribute("tree", asr.getReconstructedNewickString());

            // add msa and inferred ancestral graph
            String graphs = asr.catGraphJSONBuilder(asr.getMSAGraphJSON(), asr.getAncestralGraphJSON(asr.getInferenceType(),"root"));
            model.addAttribute("graph", graphs);

        } catch (Exception e) {
            model.addAttribute("error", "results");
            model.addAttribute("errorMessage", e.getMessage());
            System.out.println("Error: " + e.getMessage());
            return "index";
        }

        // add attribute to specify to view results (i.e. to show the graph, tree, etc)
        model.addAttribute("inferenceType", asr.getInferenceType());
        model.addAttribute("results", true);

        return "index";
    }

    /**
     * Submit the asr form (documenting input details, i.e. aln and tree file, etc)
     *
     * @param asrForm           ASR object
     * @param model             com model
     * @return                  index with results as attributes in the model
     */
    @RequestMapping(method=RequestMethod.POST, params="test")
    public String performReconstruction(@ModelAttribute ASR asrForm, Model model){

        this.asr = asrForm;

        // upload supplied files
        try {
            File sessionDir = new File(sessionPath + sessionId);
            if (!sessionDir.exists())
                sessionDir.mkdir();

            asr.setSessionDir(sessionDir.getAbsolutePath() + "/");

            asr.setLabel("Test");

            // copy default data to user session folder

            File alnFile = new File(Thread.currentThread().getContextClassLoader().getResource("default.aln").toURI());
            asr.setAlnFilepath(asr.getSessionDir() + "default.aln");
            Files.copy(alnFile.toPath(), (new File(asr.getAlnFilepath())).toPath(), StandardCopyOption.REPLACE_EXISTING);
            File treeFile = new File(Thread.currentThread().getContextClassLoader().getResource("default.nwk").toURI());
            asr.setTreeFilepath(asr.getSessionDir() + "default.nwk");
            Files.copy(treeFile.toPath(), (new File(asr.getTreeFilepath())).toPath(), StandardCopyOption.REPLACE_EXISTING);

            // TODO: push exceptions to error message on view...
            asr.runReconstruction();

            // add reconstructed newick string to send to javascript
            model.addAttribute("tree", asr.getReconstructedNewickString());

            // add msa and inferred ancestral graph
            String graphs = asr.catGraphJSONBuilder(asr.getMSAGraphJSON(), asr.getAncestralGraphJSON(asr.getInferenceType(),"root"));

            model.addAttribute("graph", graphs);

        } catch (Exception e) {
            model.addAttribute("error", "results");
            model.addAttribute("errorMessage", e.getMessage());
            System.out.println("Error: " + e.getMessage());
            return "index";
        }

        // add attribute to specify to view results (i.e. to show the graph, tree, etc)
        model.addAttribute("inferenceType", asr.getInferenceType());
        model.addAttribute("results", true);

        return "index";
    }

    /**
     * Perform marginal reconstruction of specified tree node.
     *
     * @param infer inference type (Expects marginal)
     * @param node  node label
     * @param model com model
     * @return      index view
     */
    @RequestMapping(method=RequestMethod.POST,params={"infer", "node"})
    @ResponseBody
    public String performReconstruction(@RequestParam String infer, @RequestParam String node, Model model){

        // TODO: push exceptions to error message on view...
        try {
            asr.setInferenceType(infer);

            if (infer.equalsIgnoreCase("marginal")) {
                asr.setMarginalNodeLabel(node);
                asr.runReconstruction();
            }

            // add reconstructed newick string to send to javascript
            model.addAttribute("tree", asr.getReconstructedNewickString());

        } catch (Exception e) {
            model.addAttribute("error", "results");
            model.addAttribute("errorMessage", e.getMessage());
            System.out.println("Error: " + e.getMessage());
            return "index";
        }
        // add msa and inferred ancestral graph
        String graphs = asr.catGraphJSONBuilder(asr.getMSAGraphJSON(), asr.getAncestralGraphJSON(infer, node));

        model.addAttribute("graph", graphs);
        model.addAttribute("inferenceType", asr.getInferenceType());
        model.addAttribute("results", true);

        // add attribute to specify to view results (i.e. to show the graph, tree, etc)
        return graphs;

    }
}
