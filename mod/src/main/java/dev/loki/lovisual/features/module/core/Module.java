
package dev.loki.lovisual.features.module.core;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.loki.lovisual.config.common.CommonBooleanGroupSchema;
import dev.loki.lovisual.config.common.CommonSettingSchema;
import dev.loki.lovisual.config.definition.ConfigNameProvider;
import dev.loki.lovisual.config.definition.ConfigObject;
import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.config.definition.SettingOwner;
import dev.loki.lovisual.config.i18n.ConfigValueAliasProvider;
import dev.loki.lovisual.config.values.collection.ConfigValue;
import dev.loki.lovisual.config.values.collection.ItemIdSetValue;
import dev.loki.lovisual.config.values.collection.SetValue;
import dev.loki.lovisual.config.values.color.KeyBindValue;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.config.values.mode.BindMode;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanMapValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.config.values.primitive.StringValue;
import dev.loki.lovisual.events.impl.render.RenderPrewarmCollectEvent;
import dev.loki.lovisual.features.gui.clickgui.settings.bind.FunctionBindSetting;
import dev.loki.lovisual.features.gui.clickgui.settings.bind.KeyBindSetting;
import dev.loki.lovisual.features.gui.clickgui.settings.core.Setting;
import dev.loki.lovisual.features.gui.clickgui.settings.text.list.TextListSetting;
import dev.loki.lovisual.features.module.lifecycle.ModuleActivationSource;
import dev.loki.lovisual.features.module.module.ModuleNames;
import dev.loki.lovisual.features.module.phase.HudPhase;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.SubmitNodeCollector;
import java.util.*;
import java.util.function.Supplier;

public abstract class Module implements ConfigObject, ConfigNameProvider, SettingOwner, ConfigValueAliasProvider {
    protected final List<Setting> settings = new ArrayList<>();
    private final String name; private final String displayName; private final List<String> aliases; private final String description; private final ModuleCategory category;
    private final ModuleSettingsHelper sh; private final ModuleBindHelper bh; private final ModuleLifecycleHelper lh; private final ModuleConfigHelper ch;

    protected Module() {
        ModuleInfo info = getClass().getAnnotation(ModuleInfo.class);
        if (info == null) throw new IllegalStateException(getClass().getName() + " must be annotated with @ModuleInfo");
        this.name = info.id().toLowerCase(Locale.ROOT); this.displayName = info.displayName(); this.aliases = ModuleNames.normalizeAliases(info.aliases());
        this.description = info.description(); this.category = info.category();
        this.ch = new ModuleConfigHelper(this, info.enabledByDefault()); this.sh = new ModuleSettingsHelper(this); this.bh = new ModuleBindHelper(this); this.lh = new ModuleLifecycleHelper(this);
        settings.add(bh.keyBind());
    }
    @Deprecated public Module(String n,String dn,ModuleCategory c){this(n,dn,c,"");}
    @Deprecated public Module(String n,String dn,ModuleCategory c,String d){ this.name=n.toLowerCase(Locale.ROOT); this.displayName=dn; this.aliases=List.of(); this.description=d==null?"":d; this.category=c; this.ch=new ModuleConfigHelper(this,false); this.sh=new ModuleSettingsHelper(this); this.bh=new ModuleBindHelper(this); this.lh=new ModuleLifecycleHelper(this); settings.add(bh.keyBind()); }

    public final void postInit(){ sh.postInit(settings); ModuleManager.register(this); }
    @Deprecated protected void loadSettings(List<Setting> s){}

    protected final BooleanValue bool(String cn,boolean d){return sh.bool(cn,d);} protected final BooleanValue bool(String cn,String sid,boolean d){return sh.bool(cn,sid,d);}
    protected final NumberValue<Integer> num(String cn,int d,int min,int max){return sh.num(cn,d,min,max);} protected final NumberValue<Integer> num(String cn,String sid,int d,int min,int max){return sh.num(cn,sid,d,min,max);}
    protected final NumberValue<Long> num(String cn,long d,long min,long max){return sh.numL(cn,d,min,max);} protected final NumberValue<Long> num(String cn,String sid,long d,long min,long max){return sh.numL(cn,sid,d,min,max);}
    protected final NumberValue<Float> num(String cn,float d,float min,float max){return sh.numF(cn,d,min,max);} protected final NumberValue<Float> num(String cn,String sid,float d,float min,float max){return sh.numF(cn,sid,d,min,max);}
    protected final NumberValue<Double> num(String cn,double d,double min,double max){return sh.numD(cn,d,min,max);} protected final NumberValue<Double> num(String cn,String sid,double d,double min,double max){return sh.numD(cn,sid,d,min,max);}
    protected final ModeValue mode(String cn,String d,String... o){return sh.mode(cn,d,o);} protected final ModeValue modeSetting(String cn,String sid,String d,String... o){return sh.modeSetting(cn,sid,d,o);}
    protected final <E extends Enum<E>> EnumValue<E> enumMode(String cn,E d){return sh.enumMode(cn,d);} protected final <E extends Enum<E>> EnumValue<E> enumSetting(String cn,String sid,E d){return sh.enumSetting(cn,sid,d);}
    @Deprecated protected final <E extends Enum<E>> EnumValue<E> enumMode(String cn,E d,Class<E> c){return sh.enumModeCl(cn,d,c);} @Deprecated protected final <E extends Enum<E>> EnumValue<E> enumSetting(String cn,String sid,E d,Class<E> c){return sh.enumSettingCl(cn,sid,d,c);}
    @SafeVarargs protected final <E extends Enum<E>> EnumValue<E> enumMode(String cn,E d,E... o){return sh.enumModeArr(cn,d,o);} @SafeVarargs protected final <E extends Enum<E>> EnumValue<E> enumSetting(String cn,String sid,E d,E... o){return sh.enumSettingArr(cn,sid,d,o);}
    protected final RGBAColorValue color(String cn,String h){return sh.color(cn,h);} protected final RGBAColorValue color(String cn,String sid,String h){return sh.color(cn,sid,h);}
    protected final RGBColorValue colorNoAlpha(String cn,String h){return sh.colorNoAlpha(cn,h);} protected final RGBColorValue colorNoAlpha(String cn,String sid,String h){return sh.colorNoAlpha(cn,sid,h);}
    protected final StringValue text(String cn,String d){return sh.text(cn,d);} protected final StringValue text(String cn,String sid,String d){return sh.text(cn,sid,d);}
    protected final SetValue textList(String cn){return sh.textList(cn);} protected final SetValue textList(String cn,TextListSetting.PickerMode m){return sh.textList(cn,m);}
    protected final SetValue textList(String cn,String sid,TextListSetting.PickerMode m){return sh.textList(cn,sid,m);} protected final SetValue textList(String cn,String sid,TextListSetting.PickerMode m,Set<String> defs){return sh.textList(cn,sid,m,defs);}
    protected final ItemIdSetValue itemList(String cn){return sh.itemList(cn);} protected final ItemIdSetValue itemList(String cn,TextListSetting.PickerMode m){return sh.itemList(cn,m);}
    protected final ItemIdSetValue itemList(String cn,String sid,TextListSetting.PickerMode m){return sh.itemList(cn,sid,m);}
    protected final BooleanMapValue group(String cn,Map<String,Boolean> d){return sh.group(cn,d);} protected final BooleanMapValue group(String cn,String sid,Map<String,Boolean> d){return sh.group(cn,sid,d);}
    protected final KeyBindValue bind(String cn,String d,BindMode m){return sh.bind(cn,d,m);} protected final KeyBindValue bind(String cn,String sid,String d,BindMode m){return sh.bind(cn,sid,d,m);}
    protected final BooleanMapValue groupCommon(String cn,String k,Map<String,Boolean> d){return sh.groupCommon(cn,k,d);} protected final BooleanMapValue groupCommon(String cn,String sid,String k,Map<String,Boolean> d){return sh.groupCommon(cn,sid,k,d);}
    protected final ModeValue modeCommon(String cn,String k,String d,String... o){return sh.modeCommon(cn,k,d,o);} protected final ModeValue modeCommon(String cn,String sid,String k,String d,String... o){return sh.modeCommon(cn,sid,k,d,o);}
    protected final <E extends Enum<E>> EnumValue<E> enumCommon(String cn,String k,E d){return sh.enumCommon(cn,k,d);} protected final <E extends Enum<E>> EnumValue<E> enumCommon(String cn,String sid,String k,E d){return sh.enumCommon(cn,sid,k,d);}
    protected final <E extends Enum<E>> EnumValue<E> enumCommon(String cn,String k,E d,Class<E> c){return sh.enumCommonCl(cn,k,d,c);} protected final <E extends Enum<E>> EnumValue<E> enumCommon(String cn,String sid,String k,E d,Class<E> c){return sh.enumCommonCl(cn,sid,k,d,c);}
    @SafeVarargs protected final <E extends Enum<E>> EnumValue<E> enumCommon(String cn,String k,E d,E... o){return sh.enumCommonArr(cn,k,d,o);} @SafeVarargs protected final <E extends Enum<E>> EnumValue<E> enumCommon(String cn,String sid,String k,E d,E... o){return sh.enumCommonArr(cn,sid,k,d,o);}
    protected final NumberValue<Integer> numCommon(String cn,String k,int d,int min,int max){return sh.numCommon(cn,k,d,min,max);} protected final NumberValue<Integer> numCommon(String cn,String sid,String k,int d,int min,int max){return sh.numCommon(cn,sid,k,d,min,max);}
    protected final NumberValue<Float> numCommon(String cn,String k,float d,float min,float max){return sh.numCommonF(cn,k,d,min,max);} protected final NumberValue<Float> numCommon(String cn,String sid,String k,float d,float min,float max){return sh.numCommonF(cn,sid,k,d,min,max);}
    protected final NumberValue<Double> numCommon(String cn,String k,double d,double min,double max){return sh.numCommonD(cn,k,d,min,max);} protected final NumberValue<Double> numCommon(String cn,String sid,String k,double d,double min,double max){return sh.numCommonD(cn,sid,k,d,min,max);}
    protected final BooleanValue boolCommon(String cn,String k,boolean d){return sh.boolCommon(cn,k,d);} protected final BooleanValue boolCommon(String cn,String sid,String k,boolean d){return sh.boolCommon(cn,sid,k,d);}
    protected final BooleanMapValue groupCommon(String cn,CommonBooleanGroupSchema s){return sh.groupCommon(cn,s);} protected final BooleanMapValue groupCommon(String cn,String sid,CommonBooleanGroupSchema s){return sh.groupCommon(cn,sid,s);}
    protected final BooleanMapValue protocolHeuristics(String cn,String sid,CommonBooleanGroupSchema s){return sh.protocolHeuristics(cn,sid,s);}
    protected final <E extends Enum<E>> EnumValue<E> enumCommon(String cn,CommonSettingSchema s,E d){return sh.enumCommon(cn,s,d);} protected final <E extends Enum<E>> EnumValue<E> enumCommon(String cn,String sid,CommonSettingSchema s,E d){return sh.enumCommon(cn,sid,s,d);}
    protected final <E extends Enum<E>> EnumValue<E> enumCommon(String cn,CommonSettingSchema s,E d,Class<E> c){return sh.enumCommonCl(cn,s,d,c);} protected final <E extends Enum<E>> EnumValue<E> enumCommon(String cn,String sid,CommonSettingSchema s,E d,Class<E> c){return sh.enumCommonCl(cn,sid,s,d,c);}
    @SafeVarargs protected final <E extends Enum<E>> EnumValue<E> enumCommon(String cn,CommonSettingSchema s,E d,E... o){return sh.enumCommonArr(cn,s,d,o);} @SafeVarargs protected final <E extends Enum<E>> EnumValue<E> enumCommon(String cn,String sid,CommonSettingSchema s,E d,E... o){return sh.enumCommonArr(cn,sid,s,d,o);}
    protected final ModeValue modeCommon(String cn,CommonSettingSchema s,String d,String... o){return sh.modeCommon(cn,s,d,o);} protected final ModeValue modeCommon(String cn,String sid,CommonSettingSchema s,String d,String... o){return sh.modeCommon(cn,sid,s,d,o);}
    protected final NumberValue<Integer> numCommon(String cn,CommonSettingSchema s,int d,int min,int max){return sh.numCommon(cn,s,d,min,max);} protected final NumberValue<Integer> numCommon(String cn,String sid,CommonSettingSchema s,int d,int min,int max){return sh.numCommon(cn,sid,s,d,min,max);}
    protected final NumberValue<Float> numCommon(String cn,CommonSettingSchema s,float d,float min,float max){return sh.numCommonF(cn,s,d,min,max);} protected final NumberValue<Float> numCommon(String cn,String sid,CommonSettingSchema s,float d,float min,float max){return sh.numCommonF(cn,sid,s,d,min,max);}
    protected final NumberValue<Double> numCommon(String cn,CommonSettingSchema s,double d,double min,double max){return sh.numCommonD(cn,s,d,min,max);} protected final NumberValue<Double> numCommon(String cn,String sid,CommonSettingSchema s,double d,double min,double max){return sh.numCommonD(cn,sid,s,d,min,max);}
    protected final BooleanValue boolCommon(String cn,CommonSettingSchema s,boolean d){return sh.boolCommon(cn,s,d);} protected final BooleanValue boolCommon(String cn,String sid,CommonSettingSchema s,boolean d){return sh.boolCommon(cn,sid,s,d);}
    protected final <V extends ConfigValue<?>> V visibleWhen(V v,Supplier<Boolean> s){return sh.visibleWhen(v,s);} protected final <V extends ConfigValue<?>> V shownWhen(V v,Supplier<Boolean> s){return sh.shownWhen(v,s);}
    protected final <V extends ConfigValue<?>> V unavailableWhen(V v,Supplier<Boolean> s,String r){return sh.unavailableWhen(v,s,r);} protected final <V extends ConfigValue<?>> V unavailableWhen(V v,Supplier<Boolean> s,Supplier<String> r){return sh.unavailableWhen(v,s,r);}
    protected final <V extends ConfigValue<?>> V notAppliedWhen(V v,Supplier<Boolean> s,String r){return sh.notAppliedWhen(v,s,r);} protected final <V extends ConfigValue<?>> V notAppliedWhen(V v,Supplier<Boolean> s,Supplier<String> r){return sh.notAppliedWhen(v,s,r);}
    protected final <V extends ConfigValue<?>> V appliesWhen(V v,Supplier<Boolean> s,String r){return sh.appliesWhen(v,s,r);} protected final <V extends ConfigValue<?>> V appliesWhen(V v,Supplier<Boolean> s,Supplier<String> r){return sh.appliesWhen(v,s,r);}
    protected final SettingDef setting(SettingDef d){return sh.setting(d);} public final void addExtensionSetting(SettingDef d){sh.addExtension(d,settings);}
    protected final <V extends ConfigValue<?>> V common(V v,String k){return sh.common(v,k);} protected final <V extends ConfigValue<?>> V common(V v,CommonSettingSchema s){return sh.common(v,s);}

    @Override public List<String> getLegacyValueNames(String n){return ch.legacyNames(n);}
    public List<Setting> getSettings(){return settings;} public ConfigValue<?> getConfigValue(String n){ if(n==null||n.isBlank()) return null; for(ConfigValue<?> v: getConfigValues()) if(v!=null&&n.equals(v.getName())) return v; return null; }
    public KeyBindSetting getKeyBindSetting(){return bh.keyBind();} public Collection<FunctionBindSetting> getActionSettings(){return bh.actionSettings();} public void setDefaultBind(String k){bh.setDefaultBind(k);}
    protected final FunctionBindSetting action(String n,String d,BindMode m){return bh.action(n,d,m);} protected final FunctionBindSetting action(String n,String d){return bh.action(n,d);}
    protected final void addAction(String n,String d,BindMode m){bh.addAction(n,d,m);} protected final void addAction(String n,String d){bh.addAction(n,d);}
    protected final boolean isActionPressedOnce(String n){return bh.isPressedOnce(n,lh.isEnabled());} protected final boolean isActionHeld(String n){return bh.isHeld(n,lh.isEnabled());} protected final String getActionKey(String n){return bh.actionKey(n);}
    public boolean isShownInModuleList(){return ch.isShown();} public void setShownInModuleList(boolean v){ch.setShown(v);} public void toggleShownInModuleList(){ch.toggleShown();}
    public boolean isEnabled(){return lh.isEnabled();} protected String getUnavailableReason(){return "";} public final boolean isAvailable(){return lh.isAvailable();} public final String getAvailabilityReason(){return lh.getReason();}
    public final void setEnabled(boolean s){setEnabled(s,ModuleActivationSource.MANUAL);} public ModuleActivationSource getActivationSource(){return lh.activationSource();}
    public final void setActivationSource(ModuleActivationSource s){lh.setSource(s,ch.activationValue(),ch.enabledValue());} public boolean isEnabledFromKeybind(){return lh.isFromKeybind();}
    public final void setEnabledFromKeybind(boolean s){setEnabled(s,ModuleActivationSource.KEYBIND);} public String name(){return name;} @Override public String getConfigName(){return name();}
    public String getDisplayName(){return ModuleNames.translateFirst(displayName,"module."+name(),"module."+name()+".name");} public List<String> getAliases(){return aliases;}
    public String getDescription(){return ModuleNames.translateFirst(description,"module."+name()+".description");} public ModuleCategory getCategory(){return category;}
    public final void toggle(){setEnabled(!isEnabled(),ModuleActivationSource.MANUAL);} public final void toggleFromKeybind(){setEnabled(!isEnabled(),ModuleActivationSource.KEYBIND);}
    public final void setEnabled(boolean state,ModuleActivationSource src){lh.setEnabled(state,src,ch.enabledValue(),ch.activationValue());}
    public final void setEnabledFromConfig(boolean s){setEnabled(s,ModuleActivationSource.CONFIG);} public final void setEnabledInternal(boolean s){setEnabled(s,ModuleActivationSource.INTERNAL);} public final void setEnabledManual(boolean s){setEnabled(s,ModuleActivationSource.MANUAL);}
    final void setRuntimeEnabledTransient(boolean s){lh.setTransient(s,ch.enabledValue(),ch.activationValue());}
    public void collectRenderPrewarm(RenderPrewarmCollectEvent e){ if(e==null) return; e.font("Onest",FontInfo.Type.Regular).font("OnestMedium",FontInfo.Type.Regular).font("OnestBold",FontInfo.Type.Regular).font("Inter",FontInfo.Type.Regular).font("Inter",FontInfo.Type.Bold).font("OnestMedium",FontInfo.Type.Regular).font("Iosevka",FontInfo.Type.Regular).font("Iosevka",FontInfo.Type.Bold); }
    public void onTick(){} public void onFrame(float d){} public void onKey(int k,int a){} public void onEnable(){} public void onDisable(){}
    public void onRender2D(GuiGraphicsExtractor c){} public void onRender2D(GuiGraphicsExtractor c,float d){onRender2D(c);}
    public void onRenderHudEngine(Renderer2D r,TextRenderer t){} public void onRenderHudEngine(Renderer2D r,TextRenderer t,float d){onRenderHudEngine(r,t);} public void onRenderHudEngine(Renderer2D r,TextRenderer t,GuiGraphicsExtractor c,float d){onRenderHudEngine(r,t,d);}
    public void onRenderHudEngineForeground(Renderer2D r,TextRenderer t,GuiGraphicsExtractor c,float d){}
    public void onRenderWorld(PoseStack m,SubmitNodeCollector co){} public void onRenderWorld(PoseStack m,SubmitNodeCollector co,float d){onRenderWorld(m,co);}
    public void onRenderWorldEngine(Renderer3D r,Renderer3D d){} public void onRenderWorldEngine(Renderer3D r,Renderer3D d,float t){onRenderWorldEngine(r,d);}
    public HudPhase getHudPhase(){return HudPhase.NONE;} public WorldPhase getWorldPhase(){return WorldPhase.NONE;}
    @Override public List<ConfigValue<?>> getConfigValues(){return ch.ordered(settings);}
    public void applyLegacySettings(Object j){ch.applyLegacy(j,settings);} public void syncSettingsAfterLoad(){ch.sync(settings);} public void applyConfiguredEnabledState(){setEnabled(ch.enabledValue().get(),ch.activationValue().get());}
    public void loadAndApplyConfig(){ch.loadAndApply(lh);} public void saveConfig(){ch.requestSave();}
}
