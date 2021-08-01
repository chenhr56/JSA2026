package mitm.atb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Lists {

	public static List<RecipeInfo> newArrayList(RecipeInfo recipe){
		return new ArrayList<>(Arrays.asList(recipe));
	}
	
	public static List<RecipeInfo> newArrayList(RecipeInfo recipe1, RecipeInfo recipe2, RecipeInfo recipe3, RecipeInfo recipe4){
		return new ArrayList<>(Arrays.asList(recipe1, recipe2, recipe3, recipe4));
	}
}
