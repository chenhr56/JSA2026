package factoryModel.Electrolux;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ProductionProcess {

	int id;
	String name;
	String product;
	String amount;
	int urgency;

	Map<String, String> predecessor;
	Map<String, Integer> AmountProduced;
	Map<String, List<CookingZone>> compitableResource;

	Map<String, Integer> energy;
	Map<String, Integer> processingTime;
	Map<String, Integer> montary;
	Map<String, Integer> quality;

	public ProductionProcess(int id, String name, String product, String amount, int urgency,
			Map<String, String> predecessor, Map<String, Integer> AmountProduced,
			Map<String, List<CookingZone>> compitableResource, Map<String, Integer> energy,
			Map<String, Integer> processingTime, Map<String, Integer> montary, Map<String, Integer> quality) {
		this.id = id;
		this.name = name;
		this.product = product;
		this.amount = amount;
		this.urgency = urgency;

		this.predecessor = predecessor;
		this.AmountProduced = AmountProduced;
		this.compitableResource = compitableResource;

		this.energy = energy;
		this.processingTime = processingTime;
		this.montary = montary;
		this.quality = quality;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getProduct() {
		return product;
	}

	public String getAmount() {
		return amount;
	}

	public int getUrgency() {
		return urgency;
	}

	public Map<String, List<CookingZone>> getCompitableResource() {
		return compitableResource;
	}

	public Map<String, Integer> getAmountProduced() {
		return AmountProduced;
	}

	public String getKey() {
		return name + " " + amount;
	}

	public Map<String, String> getPredecessor() {
		return predecessor;
	}

	public Map<String, Integer> getEnergy() {
		return energy;
	}

	public Map<String, Integer> getProcessingTime() {
		return processingTime;
	}

	public Map<String, Integer> getMontary() {
		return montary;
	}

	public Map<String, Integer> getQuality() {
		return quality;
	}

	@Override
	public String toString() {
		String out = "Process: " + name + " product: " + product + " amount: " + amount + " urgency: " + urgency + " \n"
				+ "Compitable Resources: " + Arrays.toString(compitableResource.entrySet().toArray()) + "\n"
				+ "Amount Produced: " + Arrays.toString(AmountProduced.entrySet().toArray()) + "\n" + "Predecessor: "
				+ Arrays.toString(predecessor.entrySet().toArray()) + "\n" + "Energy: "
				+ Arrays.toString(energy.entrySet().toArray()) + "\n" + "Processing Time: "
				+ Arrays.toString(processingTime.entrySet().toArray()) + "\n" + "Montary: "
				+ Arrays.toString(montary.entrySet().toArray()) + "\n" + "Quality: "
				+ Arrays.toString(quality.entrySet().toArray()) + "\n";
		return out;
	}

}
