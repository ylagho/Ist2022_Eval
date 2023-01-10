import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.variables.IntVar;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import escapeGameV1.BxSkill;
import escapeGameV1.EscapeGameV1Factory;
import escapeGameV1.EscapeGameV1Package;
import escapeGameV1.GameDescription;
import escapeGameV1.Level;
import escapeGameV1.ObjectiveScenario;
import escapeGameV1.Profile;
import escapeGameV1.Progress;
import escapeGameV1.Scenario;
import escapeGameV1.Skill2Consider;
import escapeGameV1.TargetedSkill;

public class GenerateObjJava {
	private Profile profileRoot = null;
	private ObjectiveScenario conceptualScenario;
	private Scenario scenario=EscapeGameV1Factory.eINSTANCE.createScenario();
	private Map<Integer, Skill2Consider> mapSkillInt= new HashMap<Integer, Skill2Consider>();
	public static void main(String[] ar){
		new GenerateObjJava().start();
	}
	public void start(){
		EscapeGameV1Package.eINSTANCE.eClass();
		String profilePath="file:///D:/eclipseModeling/workspace/IST2022/models/profile.xmi";
		String scenarioPath="file:///D:/eclipseModeling/workspace/IST2022/models/scenario.xmi";
		profileRoot= (Profile) loadModel(profilePath);
		for(Skill2Consider x: profileRoot.getSkill2consider()){
			mapSkillInt.put(Integer.parseInt(x.getBxskill().getTargetedBx().substring(1)), x);
		}
		generateConceptualScenario();
		scenario.setConceptualscenario(conceptualScenario);
		saveModel(scenarioPath,scenario);
	}
	public   EObject loadModel(String mPath){
		EObject root=null;
		Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
	    Map<String, Object> m = reg.getExtensionToFactoryMap();
	    m.put("xmi", new XMIResourceFactoryImpl());
	    m.put("ecore", new XMIResourceFactoryImpl());
	    ResourceSet resSet = new ResourceSetImpl();
	    resSet.getPackageRegistry().put(EscapeGameV1Package.eNS_URI, EscapeGameV1Package.eINSTANCE);
	    Resource resource = resSet.getResource(URI.createURI(mPath), true);
	    try {
			resource.load(Collections.EMPTY_MAP);
			root = resource.getContents().get(0);
		} catch (Exception e) {
			e.printStackTrace();
			}
		return root;
	}
	public static  void saveModel(String mPath, EObject root){
		Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
	    Map<String, Object> m = reg.getExtensionToFactoryMap();
	    m.put("xmi", new XMIResourceFactoryImpl());
	    ResourceSet resSet = new ResourceSetImpl();
	    Resource resource = resSet.createResource(URI.createURI(mPath));
	    resource.getContents().add(root);
	    try {
			resource.save(m);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private void generateConceptualScenario() {
		conceptualScenario = EscapeGameV1Factory.eINSTANCE.createObjectiveScenario();
		//Change nbLevels2generate to deal with Change 2 
		int nbLevels2generate = profileRoot.getNbLevels();
		List<Skill2Consider> bXcandidates = new ArrayList<>();
		for (Skill2Consider profileSkill: profileRoot.getSkill2consider()) {
			if (! maxLevel(profileSkill)) {
				if (profileSkill.getBxskill().getPrerequisite() == null ||  middleLevelAtLeast4(profileSkill.getBxskill().getPrerequisite())) {
					bXcandidates.add(profileSkill);
				}
			}
		}
	solveTS(bXcandidates, nbLevels2generate);
	}
	private boolean maxLevel(Skill2Consider profileSkill) {
		return profileSkill.getCurrentLevel().equals(Level.EXPERT) && profileSkill.getCurrentProgress().equals(Progress.ACQUIRED);
	}
	private boolean middleLevelAtLeast4(BxSkill skill) {
		for (Skill2Consider profileSkill: profileRoot.getSkill2consider()) {
			if ((profileSkill.getBxskill().equals(skill))) {
				//level threshold to consider a skill to be an acquired prerequisite (Change 1)
				return profileSkill.getCurrentLevel().getValue() >= Level.INTERMEDIATE_VALUE;
			}
		}
		return false;
	}
	private void solveTS(List<Skill2Consider> list, int nb){
		Model model = new Model("Targeted Skills problem allDiff");
		IntVar[] tabVar = new IntVar[nb];
		int[] values = new int[list.size()];
		int j=0;
		for(Skill2Consider sc: list){
			values[j]=Integer.parseInt(sc.getBxskill().getTargetedBx().substring(1));
			j++;
		}
		int i = 0;
		for (i=0;i<nb;i++) {
			tabVar[i] = model.intVar(values);
		}
		model.allDifferent(tabVar).post();
		List<Solution> listSol= model.getSolver().findAllSolutions(null);
		if(listSol.size()>0){
		int rand = ((int) (Math.random()*listSol.size()));
		saveSolutionTS(listSol.get(rand), tabVar);
		}
		else{
			solveTS2(list,nb);
		}
	}
	private void solveTS2(List<Skill2Consider> list, int nb){
		Model model = new Model("Targeted Skills problem following not match");
		IntVar[] tabVar = new IntVar[nb];
		int[] values = new int[list.size()];
		int j=0;
		for(Skill2Consider sc: list){
			values[j]=Integer.parseInt(sc.getBxskill().getTargetedBx().substring(1));
			j++;
		}
		int i = 0;
		for (i=0;i<nb;i++) {
			tabVar[i] = model.intVar(values);
		}
		for (int k=0; k < tabVar.length-1; k++){
				model.arithm(tabVar[k], "!=", tabVar[k+1]).post();
		}
		List<Solution> listSol= model.getSolver().findAllSolutions(null);
		if(listSol.size()>0){
		int rand = ((int) (Math.random()*listSol.size()));
		saveSolutionTS(listSol.get(rand), tabVar);
		}
		else{
			//solveTS3(list,nb);
		}
	}
	
	//added to implement Change 3
	/*
	private void solveTS3(List<Skill2Consider> list, int nb){
		Model model = new Model("Targeted Skills problem  One different");
		IntVar[] tabVar = new IntVar[nb];
		int[] values = new int[list.size()];
		int j=0;
		for(Skill2Consider sc: list){
			values[j]=Integer.parseInt(sc.getBxskill().getTargetedBx().substring(1));
			j++;
		}
		int i = 0;
		for (i=0;i<nb;i++) {
			tabVar[i] = model.intVar(values);
		}
		model.not(model.allEqual(tabVar)).post();;
		List<Solution> listSol= model.getSolver().findAllSolutions(null);
		if(listSol.size()>0){
		int rand = ((int) (Math.random()*listSol.size()));
		saveSolutionTS(listSol.get(rand), tabVar);
		}
		else{
			System.out.println("pas de solution");
		}
	}
	*/
	private void saveSolutionTS(Solution sol, IntVar[] tabVar) {
		for (int i=0; i< tabVar.length; i++) {
			TargetedSkill targetedSkill = EscapeGameV1Factory.eINSTANCE.createTargetedSkill();
			targetedSkill.setBxskill(mapSkillInt.get(sol.getIntVal(tabVar[i])).getBxskill()); 
			targetedSkill.setDifficultyLevel(mapSkillInt.get(sol.getIntVal(tabVar[i])).getCurrentLevel());
			conceptualScenario.getTargetedskill().add(targetedSkill);
		}
	}
}
