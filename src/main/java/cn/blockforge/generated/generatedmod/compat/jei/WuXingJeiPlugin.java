package cn.blockforge.generated.generatedmod.compat.jei;

import cn.blockforge.generated.generatedmod.WuXingItems;
import cn.blockforge.generated.generatedmod.alchemy.*;
import mezz.jei.api.*;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.*;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.*;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.advanced.IRecipeManagerPlugin;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import java.util.*;

/** Optional JEI integration. Only list visibility is collapsed; recipe identities remain intact. */
@JeiPlugin
public final class WuXingJeiPlugin implements IModPlugin {
    public static final IIngredientType<CatalogIngredient> TYPE=new IIngredientType<>(){
        public Class<? extends CatalogIngredient> getIngredientClass(){return CatalogIngredient.class;}
        public String getUid(){return "wuxingxiantu:quality_catalog";}
    };
    public static IJeiRuntime runtime;
    public static List<CatalogIngredient> entries(){List<CatalogIngredient> list=new ArrayList<>();for(var pill:PillCatalog.values())list.add(new CatalogIngredient(pill));list.add(new CatalogIngredient(null));return list;}
    public ResourceLocation getPluginUid(){return new ResourceLocation("wuxingxiantu","quality_catalog");}
    public void registerIngredients(IModIngredientRegistration registration){registration.register(TYPE,entries(),new Helper(),new Renderer());}
    public void registerAdvanced(IAdvancedRegistration registration){
        var factory=registration.getJeiHelpers().getFocusFactory();
        registration.addRecipeManagerPlugin(new IRecipeManagerPlugin(){
            private <V> List<IFocus<?>> real(IFocus<V> focus){List<IFocus<?>> result=new ArrayList<>();focus.checkedCast(TYPE).ifPresent(f->{
                for(var stack:f.getTypedValue().getIngredient().variants())result.add(factory.createFocus(focus.getRole(),VanillaTypes.ITEM_STACK,stack));});return result;}
            public <V> List<RecipeType<?>> getRecipeTypes(IFocus<V> focus){var real=real(focus);if(runtime==null||real.isEmpty())return List.of();List<RecipeType<?>> result=new ArrayList<>();runtime.getRecipeManager().createRecipeCategoryLookup().limitFocus(real).get().forEach(category->result.add(category.getRecipeType()));return result;}
            public <T,V> List<T> getRecipes(IRecipeCategory<T> category,IFocus<V> focus){var real=real(focus);if(runtime==null||real.isEmpty())return List.of();return runtime.getRecipeManager().createRecipeLookup(category.getRecipeType()).limitFocus(real).get().distinct().toList();}
            public <T> List<T> getRecipes(IRecipeCategory<T> category){return List.of();}
        });
    }
    public void onRuntimeAvailable(IJeiRuntime value){
        runtime=value;var manager=value.getIngredientManager();
        var hidden=manager.getAllIngredients(VanillaTypes.ITEM_STACK).stream().filter(stack->PillData.read(stack)!=null||isCore(stack)).toList();
        value.getIngredientVisibility().hideIngredients(VanillaTypes.ITEM_STACK,hidden,List.of(UidContext.Ingredient));
    }
    public void onRuntimeUnavailable(){runtime=null;}
    public static boolean isCore(ItemStack stack){for(var core:WuXingItems.INNER_CORES)if(stack.is(core.get()))return true;return false;}
    public static final class Helper implements IIngredientHelper<CatalogIngredient>{
        public IIngredientType<CatalogIngredient> getIngredientType(){return TYPE;}
        public String getDisplayName(CatalogIngredient value){return value.name().getString();}
        public String getUniqueId(CatalogIngredient value,UidContext context){return "wuxingxiantu:catalog/"+value.id();}
        public ResourceLocation getResourceLocation(CatalogIngredient value){return new ResourceLocation("wuxingxiantu",value.id());}
        public ItemStack getCheatItemStack(CatalogIngredient value){return value.cheat();}
        public CatalogIngredient copyIngredient(CatalogIngredient value){return value;}
        public String getErrorInfo(CatalogIngredient value){return value.id();}
    }
    public static final class Renderer implements IIngredientRenderer<CatalogIngredient>{
        public void render(GuiGraphics graphics,CatalogIngredient value){graphics.renderItem(value.cheat(),0,0);}
        public List<Component> getTooltip(CatalogIngredient value,TooltipFlag flag){return value.tooltip();}
    }
}
