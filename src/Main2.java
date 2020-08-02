import weka.core.*;

import java.util.*;
import java.util.stream.Collectors;

public class Main2 {

    public static void main(String[] args) {
        //ParseurArff parseurArff = new ParseurArff("resources/Jeuxsimples/tic-tac-toe.arff");
        // ParseurArff parseurArff = new ParseurArff("resources/Jeuxsimples/contact-lenses.arff");
        // ParseurArff parseurArff = new ParseurArff("resources/Jeuxsimples/coup_de_soleil.arff");

        ParseurArff parseurArff = new ParseurArff("resources/Jeuxsimples/weather.nominal.arff");
        parseurArff.parser();
        Instances instances = new Instances(parseurArff.getInstances());
        final Instances[] instancesPositifs = {new Instances(instances, 0)};
        Instances instancesNegatifs = new Instances( instances,0);
        Instances regles = new Instances( instances,0);
        // Map à un seul élément qui va contenir le meilleur candidat
        Map<Attribute,String> meilleurCandidat = new HashMap<>();
        //liste des attributs non explores
        Map<String,List<String>> candidats = new HashMap<>();
        final double[] maxGain = {-100}; // -100 valeur par défaut

        for(Instance inst:parseurArff.getInstances()){
            if(inst.classValue() == 1.0) {
                instancesNegatifs.add(inst);
            } else {
                instancesPositifs[0].add(inst);
            }

        }
        System.out.println("*********instances positifs********* \n");
        for(Instance i: instancesPositifs[0]) {
            System.out.println(i);
        }

        System.out.println("*********instances negatifs********* \n");
        for(Instance i: instancesNegatifs) {
            System.out.println(i);
        }

        //Initialisation de la liste des attributs
        for (Enumeration<Attribute> e = instancesPositifs[0].enumerateAttributes(); e.hasMoreElements();) {
            Attribute att = e.nextElement();
            List<String> valuesAtt = new ArrayList<>();

            for(Enumeration<Object> en = att.enumerateValues();en.hasMoreElements();)
            {
                String valeur = en.nextElement().toString();
                valuesAtt.add(valeur);
            }
            candidats.put(att.name(),valuesAtt);
        }

        Map<String,List<String>> map2 = candidats.entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), new ArrayList<>(e.getValue())))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        //Algorithme de foil propositionnel
        while(instancesPositifs[0].size() != 0 && !map2.isEmpty()) {
            map2.clear();
            map2 = candidats.entrySet().stream()
                    .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), new ArrayList<>(e.getValue())))
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
            int nbPG = instancesPositifs[0].numInstances();
            int nbNG = instancesNegatifs.numInstances();
            ArrayList<Attribute> attributes = new ArrayList<Attribute>(instances.numAttributes());
            for(int k = 0; k<instances.numAttributes()-1 ;++k)
            {
                attributes.add(instances.attribute(k));
            }
            attributes.add(instances.classAttribute());


            Instances posNouvelleRegle = new Instances("posNouvelleRegle",attributes,0);
            Instances negNouvelleRegle = new Instances("negNouvelleRegle",attributes,0);
            // calculer les gains de tous les candidats
            Iterator it = map2.keySet().iterator();
            while (it.hasNext()){
                String cle = (String) it.next();
                List<String> valeurs = map2.get(cle);
                System.out.print(cle+" :");
                for(String valeur:valeurs) {
                    System.out.print(valeur+' ');
                    double gain = gain(instances,cle,valeur,nbPG,nbNG);
                    System.out.print("gain: " +gain +"     ");
                    if(maxGain[0] == -100 && !Double.isNaN(gain)) {
                        maxGain[0] = gain;
                        meilleurCandidat.put(instances.attribute(cle),valeur);
                    }else {
                        if(gain >= maxGain[0] && !Double.isNaN(gain)){
                            System.out.println("****** nouveau gain max******"+maxGain[0]);
                            maxGain[0] = gain;
                            meilleurCandidat.clear();
                            meilleurCandidat.put(instances.attribute(cle),valeur);
                        }
                    }
                }
                System.out.println();
            }

            maxGain[0] = -100 ;
            for (Map.Entry<Attribute, String> entry : meilleurCandidat.entrySet()) {
                Attribute key = entry.getKey();
                String val = entry.getValue();

                //Suppression de cette valeur de la liste des candidats
                for(Iterator<String> iterator = map2.keySet().iterator(); iterator.hasNext(); ) {
                    String cle = iterator.next();
                    if(key == instances.attribute(cle) &&map2.get(cle).contains(val)) {
                        iterator.remove();
                    }
                }

                System.out.println("meilleur candidat "+key+" "+val);
                Instances nouvelleRegle = new Instances("nouvelleRegle",attributes,0);

                Instance inst = new DenseInstance(instances.numAttributes());
                inst.setValue(key,val);
                nouvelleRegle.add(inst);


                Iterator<Instance> datasetIterator = instancesPositifs[0].iterator();

                while(datasetIterator.hasNext()){

                    Instance anInstance = datasetIterator.next();
                    if(anInstance.stringValue(instances.attribute(key.name()).index()).compareTo(val) == 0)
                    {
                        posNouvelleRegle.add(anInstance);
                    }

                }


                for(Instance i : instancesNegatifs){
                    if(i.stringValue(instances.attribute(key.name()).index()) == val){
                        negNouvelleRegle.add(i);
                    }

                }
                regles.add(nouvelleRegle.get(0));
                //Supression exemples correctement couverts par la nouvelle régle
                for(Instance i : posNouvelleRegle) {
                    for (int j = instancesPositifs[0].numInstances() - 1; j >= 0; j--) {
                        Instance insta = instancesPositifs[0].get(j);
                        if ((new InstanceComparator()).compare(insta,i) == 0) {
                            instancesPositifs[0].delete(j);
                        }
                    }
                }

                System.out.println("---------instances negNewRegle----------");
                for(Instance i: negNouvelleRegle)
                {
                    System.out.println(i);
                }
                // tant que NegNewRegle != 0
                while(negNouvelleRegle.size() != 0 ) {
                    System.out.println("---------taille insNeg------------"+negNouvelleRegle.numInstances());
                    // Ajouter un vouveau literal pour specialiser NewRegle
                    Iterator it1 = map2.keySet().iterator();
                    while (it1.hasNext()){
                        String cle = (String) it1.next();
                        List<String> valeurs = map2.get(cle);
                        System.out.print(cle+" :");
                        for(String valeur:valeurs) {
                            System.out.print(valeur+' ');
                            double gain = gain(instances,cle,valeur,nbPG,nbNG);
                            System.out.print("gain: " +gain +"     ");
                            if(maxGain[0] == -100 && !Double.isNaN(gain)) {
                                maxGain[0] = gain;
                                meilleurCandidat.put(instances.attribute(cle),valeur);
                            }else {
                                if(gain >= maxGain[0] && !Double.isNaN(gain)){
                                    maxGain[0] = gain;
                                    meilleurCandidat.clear();
                                    meilleurCandidat.put(instances.attribute(cle),valeur);
                                }
                            }
                        }
                        System.out.println();
                    }

                    maxGain[0] = -100 ;

                    Map<String, List<String>> finalMap = map2;
                    meilleurCandidat.forEach((key1, val1) -> {
                        //Suppression de cette valeur de la liste des candidats
                        for(Iterator<String> iterator = finalMap.keySet().iterator(); iterator.hasNext(); ) {
                            String cle = iterator.next();
                            if(key1 == instances.attribute(cle) && finalMap.get(cle).contains(val1)) {
                                iterator.remove();
                            }
                        }

                        System.out.println("++++++++++++Candidats restants++++++++++++++++++");
                        for (Map.Entry<String, List<String>> entris : finalMap.entrySet()) {
                            System.out.print("candidat "+entris.getKey()+" ");
                            for (String ch:entris.getValue()){
                                System.out.print(ch+" ");
                            }
                            System.out.println();
                        }
                        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
                        System.out.println("meilleur candidat "+key1+" "+val1);
                        Iterator<Instance> datasetIterator1 = instancesPositifs[0].iterator();

                        //Retirer de NegNewRegle les ex qui ne satisfont pas meilleur literal
                        negNouvelleRegle.delete();
                        for(Instance i : instancesNegatifs){
                            if ((new InstanceComparator()).compare(nouvelleRegle.get(0),i) == 0) {
                                negNouvelleRegle.add(i);
                            }
                        }
                        //posNouvelleRegle.delete();
                        for (int i = posNouvelleRegle.numInstances() - 1; i >= 0; i--) {
                            Instance instan = posNouvelleRegle.get(i);
                            if (instan.stringValue(instances.attribute(key1.name()).index()) != val1) {
                                System.out.println("*******Supression posNouvelleRegle***********");
                                posNouvelleRegle.delete(i);
                            }
                        }
                        if(posNouvelleRegle.numInstances() > 3) {
                            //ajouter Meilleur_litteral aux conditions de NewRegle
                            nouvelleRegle.get(0).setValue(key1,val1);
                        }


                    });
                    meilleurCandidat.clear();
                }//negNewRegleVide
                regles.delete(regles.numInstances()-1);
                regles.add(nouvelleRegle.get(0));
            }
            meilleurCandidat.clear();
            System.out.println("-----------------------------fin d'une itération---------------------");
            instances.delete();
            instances.addAll(instancesPositifs[0]);
            instances.addAll(instancesNegatifs);
        }
        System.out.println("--------Ensembles des règles apprises");
        for(Instance i: regles)
            System.out.println(i);

    }

    public static double gain(Instances instances,String nomAttribut, String valeurAttribut,int nbPG,int nbNG) {

        int index = instances.attribute(nomAttribut).index(); //index de l'attribut dont on doit calculer le gain
        Instances instancesPositifs = new Instances(instances, 0);
        Instances instancesNegatifs = new Instances( instances,0);

        String nominalToFilter = (String)valeurAttribut;

        Instances filteredInstances = new Instances(instances, 0);
        instances.parallelStream()
                .filter(instance -> instance.stringValue(index).equals(nominalToFilter))
                .forEachOrdered(filteredInstances::add);
        for(Instance i : filteredInstances) {
            if(i.classValue() == 1.0) {
                instancesNegatifs.add(i);
            } else {
                instancesPositifs.add(i);
            }
        }
        int nbP = instancesPositifs.numInstances();
        int nbN = instancesNegatifs.numInstances();
        //calcul gain nbP*( (log(p/(nbP+nbN)) - ( log (nbPG/ (nbPG + nbNG) ) ) )
        double gain = nbP* ( ( (log2(nbP)/log2(nbP+nbN) )- (log2(nbPG)/log2(nbPG+nbNG)))) ;
        return gain;
    }

    static double log2(int x) {
        return  (Math.log(x) / Math.log(2));
    }
}