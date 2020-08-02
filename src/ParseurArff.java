import weka.core.Instances;
import weka.core.converters.ArffLoader;

import java.io.File;
import java.io.IOException;

public class ParseurArff {

	private String fichierArff =null;
	private Instances  instances = null;

	public ParseurArff( String chemin) {

		this.fichierArff = chemin;
	}

	public Instances getInstances() {
		return instances;
	}

	public void parser() {
		ArffLoader arffLoader = new ArffLoader();
		try {
			arffLoader.setFile(new File(this.fichierArff));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		try {
			instances = arffLoader.getDataSet();
			instances.setClassIndex(instances.numAttributes()-1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*
		for(Instance inst : instances){
			System.out.println("Instance:" + inst);
		}*/
	}
}
