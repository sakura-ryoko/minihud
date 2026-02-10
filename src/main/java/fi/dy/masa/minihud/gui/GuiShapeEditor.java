package fi.dy.masa.minihud.gui;

import java.util.Locale;
import java.util.function.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.gui.*;
import fi.dy.masa.malilib.gui.button.*;
import fi.dy.masa.malilib.gui.interfaces.ITextFieldListener;
import fi.dy.masa.malilib.gui.widgets.WidgetCheckBox;
import fi.dy.masa.malilib.gui.widgets.WidgetColorIndicator;
import fi.dy.masa.malilib.gui.wrappers.TextFieldType;
import fi.dy.masa.malilib.interfaces.ICoordinateValueModifier;
import fi.dy.masa.malilib.util.*;
import fi.dy.masa.malilib.util.position.PositionUtils;
import fi.dy.masa.malilib.util.position.PositionUtils.CoordinateType;
import fi.dy.masa.minihud.gui.GuiConfigs.ConfigGuiTab;
import fi.dy.masa.minihud.renderer.shapes.*;
import fi.dy.masa.minihud.util.ShapeRenderType;

public class GuiShapeEditor extends GuiRenderLayerEditBase
{
    private final ShapeBase shape;
    private final ConfigOptionList configBlockSnap;
    private int colorY;

    public GuiShapeEditor(ShapeBase shape)
    {
        this.shape = shape;
        this.title = StringUtils.translate("minihud.gui.title.shape_editor", shape.getDisplayName());
        this.configBlockSnap = new ConfigOptionList("blockSnap", BlockSnap.NONE, "");
    }

    @Override
    public void initGui()
    {
        super.initGui();

        int x = 10;
        int y = 20;

        this.createShapeEditorElements(x, y);

        ButtonGeneric button = new ButtonGeneric(x, this.getScreenHeight() - 24, -1, 20, ConfigGuiTab.SHAPES.getDisplayName());
        this.addButton(button, new GuiShapeManager.ButtonListenerTab(ConfigGuiTab.SHAPES));

        ButtonOnOff renderThroughButton = new ButtonOnOff(this.getScreenWidth() - 224, this.getScreenHeight() - 24, -1, false, "minihud.gui.button.shape_renderer.toggle_render_through", this.shape.shouldRenderThrough());
        this.addButton(renderThroughButton, (b, mb) -> this.toggleRenderThrough(shape, renderThroughButton));

        this.createColorOutlinesInput(this.getScreenWidth() - 98, this.getScreenHeight() - 60);

        ButtonOnOff renderLinesButton = new ButtonOnOff(this.getScreenWidth() - 104, this.getScreenHeight() - 24, -1, false, "minihud.gui.button.shape_renderer.toggle_render_lines", this.shape.shouldRenderLines());
        this.addButton(renderLinesButton, (b, mb) -> this.toggleRenderLines(shape, renderLinesButton));
    }

    @Override
    protected LayerRange getLayerRange()
    {
        return this.shape.getLayerRange();
    }

    private void createColorInput(int x, int y)
    {
        this.addLabel(x, y, -1, 14, 0xFFFFFFFF, StringUtils.translate("minihud.gui.label.color"));
        y += 12;

        GuiTextFieldGeneric textField = new GuiTextFieldGeneric(x, y, 70, 17, this.font);
        textField.setMaxLengthWrapper(12);
        textField.setValueWrapper(String.format("#%08X", this.shape.getColor().intValue));
        this.addTextField(textField, new TextFieldListenerColor(this.shape), TextFieldType.STRING);
        this.nextY = y + 20;
        this.colorY = y - 1;

        this.addWidget(new WidgetColorIndicator(x + 74, this.colorY, 19, 19, this.shape.getColor(), this.shape::setColor));
    }

    private void createColorOutlinesInput(int x, int y)
    {
        this.addLabel(x, y, -1, 14, 0xFFFFFFFF, StringUtils.translate("minihud.gui.label.outlines_color"));
        y += 12;

        GuiTextFieldGeneric textField = new GuiTextFieldGeneric(x, y, 70, 17, this.font);
        textField.setMaxLengthWrapper(12);
        textField.setValueWrapper(String.format("#%08X", this.shape.getColorLines().intValue));
        this.addTextField(textField, new TextFieldListenerColorLines(this.shape), TextFieldType.STRING);
        this.nextY = y + 20;
        this.colorY = y - 1;

        this.addWidget(new WidgetColorIndicator(x + 74, this.colorY, 19, 19, this.shape.getColorLines(), this.shape::setColorLines));
    }

    private void createShapeEditorElements(int x, int y)
    {
        this.addLabel(x, y, -1, 14, 0xFFFFFFFF, StringUtils.translate("minihud.gui.label.display_name_colon"));
        y += 12;

        GuiTextFieldGeneric textField = new GuiTextFieldGeneric(x, y, 240, 17, this.font);
        textField.setValueWrapper(this.shape.getDisplayName());
        this.addTextField(textField, (txtFld) -> { this.shape.setDisplayName(txtFld.getValueWrapper()); return true; }, TextFieldType.STRING);
        y += 20;

        int renderTypeX = x + 230;
        int renderTypeY = y + 2;
        ShapeType type = this.shape.getType();

        switch (type)
        {
           
            case BOX:
                this.createShapeEditorElementsBox(x, y);
                break;
            case CENTERED_BOX:
                this.createShapeEditorElementsBoxWithDimension(x, y);
                break;
            case BLOCK_LINE:
                this.createShapeEditorElementsBlockLine(x, y);
                break;

            case ADJUSTABLE_SPAWN_SPHERE:
            case CAN_DESPAWN_SPHERE:
            case CAN_SPAWN_SPHERE:
            case DESPAWN_SPHERE:
            {
                ShapeSpawnSphere shape = (ShapeSpawnSphere) this.shape;
                boolean isAdjustable = type == ShapeType.ADJUSTABLE_SPAWN_SPHERE;
                this.createShapeEditorElementsSphereBase(x, y, isAdjustable);

                if (isAdjustable == false)
                {
                    this.createShapeEditorElementDoubleField(x + 150, y + 2, shape::getMargin, shape::setMargin, "minihud.gui.label.margin_colon", false);
                }

                if (shape instanceof ShapeSpawnSphere)
                {
                    String key = "minihud.gui.button.shape_renderer.spawn_sphere.toggle_use_quadrants";
                    String hover = StringUtils.translate("minihud.gui.button.hover.shape_renderer.spawn_sphere.toggle_use_quadrants");
                    ButtonOnOff button = new ButtonOnOff(x + 160, y + 30, -1, false, key, shape.getUseCornerQuadrants(), hover);
                    this.addButton(button, (btn, mbtn) -> this.toggleUseQuadrants(shape, button));
                }

//                this.createLayerEditControls(146, 162, this.getLayerRange());
                break;
            }

            case CIRCLE:
            {
                ShapeCircle shape = (ShapeCircle) this.shape;
                this.createShapeEditorElementsSphereBase(x, y, true);
                this.createShapeEditorElementIntField(x + 150, y + 36, shape::getHeight, shape::setHeight, "minihud.gui.label.height_colon", true);
                this.createDirectionButton(x + 230, y + 36, shape::getMainAxis, shape::setMainAxis, "minihud.gui.label.shape.circle.main_axis_colon");
                this.createRenderTypeButton(renderTypeX, renderTypeY, this.shape::getRenderType, this.shape::setRenderType, "minihud.gui.label.shape.render_type_colon");
//                this.createLayerEditControls(146, 162, this.getLayerRange());
                break;
            }

            case SPHERE_BLOCKY:
                this.createShapeEditorElementsSphereBase(x, y, true);
                this.createRenderTypeButton(renderTypeX, renderTypeY, this.shape::getRenderType, this.shape::setRenderType, "minihud.gui.label.shape.render_type_colon");
//                this.createLayerEditControls(146, 162, this.getLayerRange());
                break;

            case ELLIPSOID_SPAWN:
            {
                ShapeEllipsoidSpawn shape = (ShapeEllipsoidSpawn) this.shape;
                this.createShapeEditorElementsSphereBase(x, y, true);
                this.createShapeEditorElementDoubleField(x + 150, y + 36, shape::getRadiusY, shape::setRadiusY, "minihud.gui.label.radius_y_colon", true);
                this.createShapeEditorElementDoubleField(x + 220, y + 36, shape::getRadiusZ, shape::setRadiusZ, "minihud.gui.label.radius_z_colon", true);
                this.createRenderTypeButton(renderTypeX, renderTypeY, this.shape::getRenderType, this.shape::setRenderType, "minihud.gui.label.shape.render_type_colon");
                break;
            }

            case CLIPPED_SPAWN_SPHERE_Y:
            {
                ShapeSpawnSphereClippedY shape = (ShapeSpawnSphereClippedY) this.shape;
                this.createShapeEditorElementsSphereBase(x, y, true);
                this.createShapeEditorElementDoubleField(x + 150, y + 36, shape::getTopTrim, shape::setTopTrim, "minihud.gui.label.clip_top_colon", true);
                this.createShapeEditorElementDoubleField(x + 220, y + 36, shape::getBottomTrim, shape::setBottomTrim, "minihud.gui.label.clip_bottom_colon", true);
                this.createRenderTypeButton(renderTypeX, renderTypeY, this.shape::getRenderType, this.shape::setRenderType, "minihud.gui.label.shape.render_type_colon");
                break;
            }
        }
    }

    private void createShapeEditorElementsSphereBase(int x, int y, boolean addRadiusInput)
    {
        ShapeCircleBase shape = (ShapeCircleBase) this.shape;

        this.addLabel(x, y, 60, 14, 0xFFFFFFFF, StringUtils.translate("minihud.gui.label.center_colon"));

        if (addRadiusInput)
        {
            this.createShapeEditorElementDoubleField(x + 150, y + 2, shape::getRadius, shape::setRadius, "minihud.gui.label.radius_colon", true);
        }

        y += 12;
        GuiUtils.createVec3dInputsVertical(x, y, 120, shape.getEffectiveCenter(), new Vec3dEditor(shape::getEffectiveCenter, shape::setCenter, this), true, this);
        x += 11;
        y += 54;

        int btnX = x;
        ButtonGeneric button = new ButtonGeneric(btnX, y, -1, false, "malilib.gui.button.render_layers_gui.set_to_player");
        this.addButton(button, (btn, mbtn) -> this.setPositionFromCamera(shape::setCenter));
        btnX = button.getX() + button.getWidth() + 4;

        this.configBlockSnap.setOptionListValue(shape.getBlockSnap());
        String label = StringUtils.translate("minihud.gui.label.shape.block_snap", shape.getBlockSnap().getDisplayName());
        int width = this.getStringWidth(label) + 10;

        ConfigButtonOptionList buttonSnap = new ConfigButtonOptionList(btnX, y, width, 20, this.configBlockSnap, label);
        this.addButton(buttonSnap, new ButtonListenerSphereBlockSnap(shape, this));
        btnX = buttonSnap.getX() + buttonSnap.getWidth() + 4;

        ButtonOnOff combineQuadsButton = new ButtonOnOff(btnX, y, -1, false, "minihud.gui.button.shape_renderer.toggle_combine_quads", ((ShapeBlocky) this.shape).getCombineQuads());
        this.addButton(combineQuadsButton, (b, mb) -> this.toggleCombineQuads(shape, combineQuadsButton));
        y += 24;

        this.createColorInput(x, y);
        y += 11;

        this.createLayerEditControls(146, y, this.getLayerRange());
    }   

    public void createCenteredBoxInputs(int x1, int y1, int x2, int y2,int x3,int y3,int x4,int y4, int textFieldWidth, ShapeCenteredBox shape)
    {
        this.createShapeEditorElementDoubleField(x1, y1, shape::getHeight, shape::setHeight, "minihud.gui.label.height_colon", true);
        this.createShapeEditorElementDoubleField(x2, y2, shape::getWidth, shape::setWidth, "minihud.gui.label.width_colon", true);
        this.createShapeEditorElementDoubleField(x3, y3, shape::getDepth, shape::setDepth, "minihud.gui.label.depth_colon", true);
        //TODO: Maybe Change that +12 to 0 and reset to 0 also createShapeEditorElementIntField or add some type of offset
        this.addLabel(x4 + 12, y4, -1, 12, 0xFFFFFFFF, StringUtils.translate("minihud.gui.label.center_colon"));
        y4 += 12;
        GuiUtils.createVec3dInputsVertical(x4, y4, textFieldWidth, shape.getCenter(), new Vec3dEditor(shape::getCenter, shape::setCenter, this), true, this);

        int x = x1 + 12;
        ButtonGeneric btn = new ButtonGeneric(x, y4 + 50, -1, 14, StringUtils.translate("malilib.gui.button.render_layers_gui.set_to_player"));
        btn.setRenderDefaultBackground(false);
        this.addButton(btn, (b, mb) -> this.setPositionFromCamera(shape::setCenter));
    }

    private void createShapeEditorElementsBoxWithDimension(int xIn, int yIn)
	{
        ShapeCenteredBox shape = (ShapeCenteredBox) this.shape;

        int x = xIn;
        int y = yIn + 4;

        this.createCenteredBoxInputs(x, y,  x, y + 28, x, y + 56, x, y + 84, 120, shape);

        y += 160;
        this.createColorInput(x + 12, y);

        x = xIn + 250;
        y = yIn + 4;
        this.addBoxSideToggleCheckbox(x, y     , Direction.DOWN,  shape);
        this.addBoxSideToggleCheckbox(x, y + 11, Direction.UP,    shape);
        this.addBoxSideToggleCheckbox(x, y + 22, Direction.NORTH, shape);
        this.addBoxSideToggleCheckbox(x, y + 33, Direction.SOUTH, shape);
        this.addBoxSideToggleCheckbox(x, y + 44, Direction.WEST,  shape);
        this.addBoxSideToggleCheckbox(x, y + 55, Direction.EAST,  shape);

        x = xIn + 160;
        y = yIn + 4;

        if (shape.isGridEnabled())
        {
            this.addLabel(x, y, 60, 14, 0xFFFFFFFF, StringUtils.translate("minihud.gui.label.shape.box.grid_size"));
            GuiUtils.createVec3dInputsVertical(x, y + 12, 50, shape.getGridSize(),
                                               new Vec3dEditor(shape::getGridSize, shape::setGridSize, this), true, this);

            y += 70;
            this.addLabel(x, y, 60, 14, 0xFFFFFFFF, StringUtils.translate("minihud.gui.label.shape.box.grid_start_offset"));
            GuiUtils.createVec3dInputsVertical(x, y + 12, 50, shape.getGridStartOffset(),
                                               new Vec3dEditor(shape::getGridStartOffset, shape::setGridStartOffset, this), true, this);

            this.addLabel(x + 100, y, 60, 14, 0xFFFFFFFF, StringUtils.translate("minihud.gui.label.shape.box.grid_end_offset"));
            GuiUtils.createVec3dInputsVertical(x + 100, y + 12, 50, shape.getGridEndOffset(),
                                               new Vec3dEditor(shape::getGridEndOffset, shape::setGridEndOffset, this), true, this);
        }

        y = yIn + 148;
        ButtonGeneric button = new ButtonOnOff(x, y, -1, false, "minihud.gui.label.shape.box.grid_enabled", shape.isGridEnabled());
        this.addButton(button, (btn, mbtn) -> this.toggleGridEnabled(shape));
    }


    private void createShapeEditorElementsBox(int xIn, int yIn)
    {
        ShapeBox shape = (ShapeBox) this.shape;

        int x = xIn;
        int y = yIn + 4;

        this.createBoxInputs(x, y, x, y + 82, 120, shape);

        y += 160;
        this.createColorInput(x + 12, y);

        x = xIn + 250;
        y = yIn + 4;
        this.addBoxSideToggleCheckbox(x, y     , Direction.DOWN,  shape);
        this.addBoxSideToggleCheckbox(x, y + 11, Direction.UP,    shape);
        this.addBoxSideToggleCheckbox(x, y + 22, Direction.NORTH, shape);
        this.addBoxSideToggleCheckbox(x, y + 33, Direction.SOUTH, shape);
        this.addBoxSideToggleCheckbox(x, y + 44, Direction.WEST,  shape);
        this.addBoxSideToggleCheckbox(x, y + 55, Direction.EAST,  shape);

        x = xIn + 160;
        y = yIn + 4;

        if (shape.isGridEnabled())
        {
            this.addLabel(x, y, 60, 14, 0xFFFFFFFF, StringUtils.translate("minihud.gui.label.shape.box.grid_size"));
            GuiUtils.createVec3dInputsVertical(x, y + 12, 50, shape.getGridSize(),
                                               new Vec3dEditor(shape::getGridSize, shape::setGridSize, this), true, this);

            y += 70;
            this.addLabel(x, y, 60, 14, 0xFFFFFFFF, StringUtils.translate("minihud.gui.label.shape.box.grid_start_offset"));
            GuiUtils.createVec3dInputsVertical(x, y + 12, 50, shape.getGridStartOffset(),
                                               new Vec3dEditor(shape::getGridStartOffset, shape::setGridStartOffset, this), true, this);

            this.addLabel(x + 100, y, 60, 14, 0xFFFFFFFF, StringUtils.translate("minihud.gui.label.shape.box.grid_end_offset"));
            GuiUtils.createVec3dInputsVertical(x + 100, y + 12, 50, shape.getGridEndOffset(),
                                               new Vec3dEditor(shape::getGridEndOffset, shape::setGridEndOffset, this), true, this);
        }

        y = yIn + 148;
        ButtonGeneric button = new ButtonOnOff(x, y, -1, false, "minihud.gui.label.shape.box.grid_enabled", shape.isGridEnabled());
        this.addButton(button, (btn, mbtn) -> this.toggleGridEnabled(shape));
    }

    private void createShapeEditorElementsBlockLine(int xIn, int yIn)
    {
        ShapeLineBlock shape = (ShapeLineBlock) this.shape;

        int x = xIn;
        int x2 = x + 160;
        int y = yIn + 4;

        this.addLabel(x, y, -1, 14, 0xFFFFFFFF, StringUtils.translate("minihud.gui.label.shape.box.minimum_coord"));
        this.addLabel(x2, y, -1, 14, 0xFFFFFFFF, StringUtils.translate("minihud.gui.label.shape.box.maximum_coord"));
        y += 14;

        GuiUtils.createVec3dInputsVertical(x , y, 120, shape.getStartPos(), new Vec3dEditor(shape::getStartPos, shape::setStartPos, this), true, this);
        GuiUtils.createVec3dInputsVertical(x2, y, 120, shape.getEndPos(), new Vec3dEditor(shape::getEndPos, shape::setEndPos, this), true, this);
        y += 54;

        ButtonGeneric btn = new ButtonGeneric(x + 11, y, -1, 20, StringUtils.translate("malilib.gui.button.render_layers_gui.set_to_player"));
        this.addButton(btn, (b, mb) -> this.setPositionFromCamera(shape::setStartPos));

        btn = new ButtonGeneric(x2 + 11, y, -1, 20, StringUtils.translate("malilib.gui.button.render_layers_gui.set_to_player"));
        this.addButton(btn, (b, mb) -> this.setPositionFromCamera(shape::setEndPos));
        y += 24;

        int btnX = xIn + 11;
        this.configBlockSnap.setOptionListValue(shape.getBlockSnap());
        String label = StringUtils.translate("minihud.gui.label.shape.block_snap", shape.getBlockSnap().getDisplayName());
        int width = this.getStringWidth(label) + 10;

        ConfigButtonOptionList buttonSnap = new ConfigButtonOptionList(btnX, y, width, 20, this.configBlockSnap, label);
        this.addButton(buttonSnap, new ButtonListenerSphereBlockSnap(shape, this));
        btnX += buttonSnap.getWidth() + 4;

        ButtonOnOff combineQuadsButton = new ButtonOnOff(btnX, y, -1, false, "minihud.gui.button.shape_renderer.toggle_combine_quads", ((ShapeBlocky) this.shape).getCombineQuads());
        this.addButton(combineQuadsButton, (b, mb) -> this.toggleCombineQuads(shape, combineQuadsButton));
        y += 24;

        this.createColorInput(xIn + 12, y);
        y += 11;

        this.createLayerEditControls(xIn + 115, y, this.getLayerRange());
    }

    private void toggleGridEnabled(ShapeBox shape)
    {
        shape.toggleGridEnabled();
        this.initGui();
    }

    private void toggleCombineQuads(ShapeBlocky shape, ButtonOnOff button)
    {
        shape.toggleCombineQuads();
        button.updateDisplayString(shape.getCombineQuads());
    }

    private void toggleUseQuadrants(ShapeSpawnSphere shape, ButtonOnOff button)
    {
        shape.toggleUseCornerQuadrants();
        button.updateDisplayString(shape.getUseCornerQuadrants());
    }

    private void addBoxSideToggleCheckbox(int x, int y, Direction side, ShapeBox shape)
    {
        String sideName = StringUtils.translate("minihud.gui.name.box.box_side." + side.name().toLowerCase());
        WidgetCheckBox cb = new WidgetCheckBox(x, y, MaLiLibIcons.MINUS, MaLiLibIcons.PLUS, this.capitalize(sideName), StringUtils.translate("minihud.gui.hover.shape.box.box_side", sideName));
        cb.setChecked(shape.isSideEnabled(side));
        cb.setListener((w) -> this.toggleSideEnabled(side, shape));
        this.addWidget(cb);
    }

    private void toggleSideEnabled(Direction side, ShapeBox shape)
    {
        int mask = shape.getEnabledSidesMask();
        shape.setEnabledSidesMask(mask ^ (1 << side.get3DDataValue()));
    }

    private void toggleRenderLines(ShapeBase shape, ButtonOnOff button)
    {
        shape.toggleRenderLines();
        button.updateDisplayString(shape.shouldRenderLines());
    }

    private void toggleRenderThrough(ShapeBase shape, ButtonOnOff button)
    {
        shape.toggleRenderThrough();
        button.updateDisplayString(shape.shouldRenderThrough());
    }

    public void createBoxInputs(int x1, int y1, int x2, int y2, int textFieldWidth, ShapeBox shape)
    {
        this.addLabel(x1, y1, -1, 14, 0xFFFFFFFF, StringUtils.translate("minihud.gui.label.shape.box.corner_1"));
        y1 += 12;

        this.addLabel(x2, y2, -1, 14, 0xFFFFFFFF, StringUtils.translate("minihud.gui.label.shape.box.corner_2"));
        y2 += 12;

        GuiUtils.createVec3dInputsVertical(x1, y1, textFieldWidth, shape.getCorner1(), new Vec3dEditor(shape::getCorner1, shape::setCorner1, this), true, this);
        GuiUtils.createVec3dInputsVertical(x2, y2, textFieldWidth, shape.getCorner2(), new Vec3dEditor(shape::getCorner2, shape::setCorner2, this), true, this);

        int x = x1 + 12;

        ButtonGeneric btn = new ButtonGeneric(x, y1 + 50, -1, 14, StringUtils.translate("malilib.gui.button.render_layers_gui.set_to_player"));
        btn.setRenderDefaultBackground(false);
        this.addButton(btn, (b, mb) -> this.setPositionFromCamera(shape::setCorner1));

        btn = new ButtonGeneric(x, y2 + 50, -1, 14, StringUtils.translate("malilib.gui.button.render_layers_gui.set_to_player"));
        btn.setRenderDefaultBackground(false);
        this.addButton(btn, (b, mb) -> this.setPositionFromCamera(shape::setCorner2));
    }

    protected void addBoxInput(int x, int y, int textFieldWidth, DoubleSupplier coordinateSource,
                               DoubleConsumer coordinateConsumer)
    {
        Font textRenderer = Minecraft.getInstance().font;
        GuiTextFieldGeneric textField = new GuiTextFieldGeneric(x, y + 1, textFieldWidth, 14, textRenderer);
        textField.setValueWrapper("" + coordinateSource.getAsDouble());

        this.addTextFieldAndButtonForBoxCoordinate(x + textFieldWidth + 4, y, textField,
                                                   coordinateSource, coordinateConsumer);
    }

    protected int addLabel(int x, int y, CoordinateType type)
    {
        String label = type.name() + ":";
        int labelWidth = 12;
        this.addLabel(x, y, labelWidth, 20, 0xFFFFFFFF, label);
        return labelWidth;
    }

    protected void addTextFieldAndButtonForBoxCoordinate(int x, int y, GuiTextFieldGeneric textField,
                                                                DoubleSupplier coordinateSource,
                                                                DoubleConsumer coordinateConsumer)
    {
        this.addTextField(textField, new TextFieldListenerDouble(coordinateConsumer), TextFieldType.DOUBLE);

        String hover = StringUtils.translate("malilib.gui.button.hover.plus_minus_tip");
        ButtonGeneric button = new ButtonGeneric(x, y, MaLiLibIcons.BTN_PLUSMINUS_16, hover);
        this.addButton(button, new ButtonListenerDoubleModifier(coordinateSource, (v) -> {
            coordinateConsumer.accept(v);
            textField.setValueWrapper("" + coordinateSource.getAsDouble());
        }));
    }

    private String capitalize(String str)
    {
        if (str.length() > 1)
        {
            return str.substring(0, 1).toUpperCase(Locale.ROOT) + str.substring(1);
        }

        return str.length() > 0 ? str.toUpperCase(Locale.ROOT) : str;
    }

    private void createShapeEditorElementDoubleField(int x, int y, DoubleSupplier supplier, DoubleConsumer consumer, String translationKey, boolean addButton)
    {
        this.addLabel(x + 12, y, -1, 12, 0xFFFFFFFF, translationKey);
        y += 11;

        GuiTextFieldDouble txtField = new GuiTextFieldDouble(x + 12, y, 40, 14, this.font);
        txtField.setValueWrapper(String.valueOf(supplier.getAsDouble()));
        this.addTextField(txtField, new TextFieldListenerDouble(consumer), TextFieldType.DOUBLE);

        if (addButton)
        {
            String hover = StringUtils.translate("malilib.gui.button.hover.plus_minus_tip");
            ButtonGeneric button = new ButtonGeneric(x + 54, y - 1, MaLiLibIcons.BTN_PLUSMINUS_16, hover);
            this.addButton(button, new ButtonListenerDoubleModifier(supplier, new ChainedDoubleConsumer(consumer, (val) -> txtField.setValueWrapper(String.valueOf(supplier.getAsDouble())) )));
        }
    }

    private void createShapeEditorElementIntField(int x, int y, IntSupplier supplier, IntConsumer consumer, String translationKey, boolean addButton)
    {
        this.addLabel(x + 12, y, -1, 12, 0xFFFFFFFF, translationKey);
        y += 11;

        GuiTextFieldInteger txtField = new GuiTextFieldInteger(x + 12, y, 40, 14, this.font);
        txtField.setValueWrapper(String.valueOf(supplier.getAsInt()));
        this.addTextField(txtField, new TextFieldListenerInteger(consumer), TextFieldType.INTEGER);

        if (addButton)
        {
            String hover = StringUtils.translate("malilib.gui.button.hover.plus_minus_tip");
            ButtonGeneric button = new ButtonGeneric(x + 54, y - 1, MaLiLibIcons.BTN_PLUSMINUS_16, hover);
            this.addButton(button, new ButtonListenerIntModifier(supplier, new ChainedIntConsumer(consumer, (val) -> txtField.setValueWrapper(String.valueOf(supplier.getAsInt())) )));
        }
    }

    private void createDirectionButton(int x, int y, Supplier<Direction> supplier, Consumer<Direction> consumer, String translationKey)
    {
        this.addLabel(x, y, -1, 12, 0xFFFFFFFF, translationKey);
        y += 10;

        ButtonGeneric button = new ButtonGeneric(x, y, 50, 20, org.apache.commons.lang3.StringUtils.capitalize(supplier.get().toString().toLowerCase()));
        this.addButton(button, (btn, mouseBtn) -> { consumer.accept(cycleDirection(supplier.get(), mouseBtn == 1)); this.initGui(); } );
    }

    private void createRenderTypeButton(int x, int y, Supplier<ShapeRenderType> supplier, Consumer<ShapeRenderType> consumer, String translationKey)
    {
        this.addLabel(x, y, -1, 12, 0xFFFFFFFF, translationKey);
        y += 10;

        ButtonGeneric button = new ButtonGeneric(x, y, -1, 20, supplier.get().getDisplayName());
        this.addButton(button, (btn, mouseBtn) -> { consumer.accept((ShapeRenderType) supplier.get().cycle(mouseBtn == 0)); this.initGui(); } );
    }

    public static Direction cycleDirection(Direction direction, boolean reverse)
    {
        int index = direction.get3DDataValue();

        if (reverse)
        {
            index = index == 0 ? 5 : index - 1;
        }
        else
        {
            index = index >= 5 ? 0 : index + 1;
        }

        return Direction.from3DDataValue(index);
    }

    protected void setPositionFromCamera(Consumer<Vec3> consumer)
    {
        Entity entity = EntityUtils.getCameraEntity();

        if (entity != null)
        {
            consumer.accept(entity.position());
            this.initGui();
        }
    }

    protected void setBlockPosFromCamera(Consumer<BlockPos> consumer)
    {
        Entity entity = EntityUtils.getCameraEntity();

        if (entity != null)
        {
            consumer.accept(entity.blockPosition());
            this.initGui();
        }
    }

    public static class MutableWrapperBox
    {
        protected final Consumer<AABB> boxConsumer;
        protected double minX;
        protected double minY;
        protected double minZ;
        protected double maxX;
        protected double maxY;
        protected double maxZ;

        public MutableWrapperBox(AABB box, Consumer<AABB> boxConsumer)
        {
            this.minX = box.minX;
            this.minY = box.minY;
            this.minZ = box.minZ;
            this.maxX = box.maxX;
            this.maxY = box.maxY;
            this.maxZ = box.maxZ;
            this.boxConsumer = boxConsumer;
        }

        public double getMinX()
        {
            return this.minX;
        }

        public double getMinY()
        {
            return this.minY;
        }

        public double getMinZ()
        {
            return this.minZ;
        }

        public double getMaxX()
        {
            return this.maxX;
        }

        public double getMaxY()
        {
            return this.maxY;
        }

        public double getMaxZ()
        {
            return this.maxZ;
        }

        public void setMinX(double minX)
        {
            this.minX = minX;
            this.updateBox();
        }

        public void setMinY(double minY)
        {
            this.minY = minY;
            this.updateBox();
        }

        public void setMinZ(double minZ)
        {
            this.minZ = minZ;
            this.updateBox();
        }

        public void setMaxX(double maxX)
        {
            this.maxX = maxX;
            this.updateBox();
        }

        public void setMaxY(double maxY)
        {
            this.maxY = maxY;
            this.updateBox();
        }

        public void setMaxZ(double maxZ)
        {
            this.maxZ = maxZ;
            this.updateBox();
        }

        public void setMinCorner(Vec3 pos)
        {
            this.minX = pos.x;
            this.minY = pos.y;
            this.minZ = pos.z;
            this.updateBox();
        }

        public void setMaxCorner(Vec3 pos)
        {
            this.maxX = pos.x;
            this.maxY = pos.y;
            this.maxZ = pos.z;
            this.updateBox();
        }

        protected void updateBox()
        {
            AABB box = new AABB(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
            this.boxConsumer.accept(box);
        }
    }

    public record Vec3dEditor(Supplier<Vec3> supplier, Consumer<Vec3> consumer, GuiShapeEditor gui) implements ICoordinateValueModifier
    {
        @Override
        public boolean modifyValue(CoordinateType type, int amount)
        {
            this.consumer.accept(PositionUtils.modifyValue(type, this.supplier.get(), amount));
            this.gui.initGui();
            return true;
        }

        @Override
        public boolean setValueFromString(CoordinateType type, String newValue)
        {
            try
            {
                this.consumer.accept(PositionUtils.setValue(type, this.supplier.get(), Double.parseDouble(newValue)));
                return true;
            }
            catch (Exception ignore) {}

            return false;
        }
    }

    public record BlockPosEditor(Supplier<BlockPos> supplier, Consumer<BlockPos> consumer, GuiShapeEditor gui) implements ICoordinateValueModifier
    {
        @Override
        public boolean modifyValue(CoordinateType type, int amount)
        {
            this.consumer.accept(PositionUtils.modifyValue(type, this.supplier.get(), amount));
            this.gui.initGui();
            return true;
        }

        @Override
        public boolean setValueFromString(CoordinateType type, String newValue)
        {
            try
            {
                this.consumer.accept(PositionUtils.setValue(type, this.supplier.get(), Integer.parseInt(newValue)));
                return true;
            }
            catch (Exception ignore) {}

            return false;
        }
    }

    private record ButtonListenerSphereBlockSnap(ShapeBlocky shape, GuiShapeEditor gui) implements IButtonActionListener
    {
        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            this.shape.setBlockSnap((BlockSnap) this.gui.configBlockSnap.getOptionListValue());
            this.gui.initGui();
        }
    }

    private record TextFieldListenerColor(ShapeBase shape) implements ITextFieldListener<GuiTextFieldGeneric>
    {
        @Override
        public boolean onTextChange(GuiTextFieldGeneric textField)
        {
            this.shape.setColorFromString(textField.getValueWrapper());
            return false;
        }
    }

    private record TextFieldListenerColorLines(ShapeBase shape) implements ITextFieldListener<GuiTextFieldGeneric>
    {
        @Override
        public boolean onTextChange(GuiTextFieldGeneric textField)
        {
            this.shape.setColorLinesFromString(textField.getValueWrapper());
            return false;
        }
    }

    private record TextFieldListenerInteger(IntConsumer consumer) implements ITextFieldListener<GuiTextFieldInteger>
    {
        @Override
        public boolean onTextChange(GuiTextFieldInteger textField)
        {
            try
            {
                this.consumer.accept(Integer.parseInt(textField.getValueWrapper()));
                return true;
            }
            catch (Exception ignore) {}

            return false;
        }
    }

    private record TextFieldListenerDouble(DoubleConsumer consumer) implements ITextFieldListener<GuiTextFieldGeneric>
    {
        @Override
        public boolean onTextChange(GuiTextFieldGeneric textField)
        {
            try
            {
                this.consumer.accept(Double.parseDouble(textField.getValueWrapper()));
                return true;
            }
            catch (Exception ignore) {}

            return false;
        }
    }

    private record ChainedDoubleConsumer(DoubleConsumer consumerOne, DoubleConsumer consumerTwo) implements DoubleConsumer
    {
        @Override
        public void accept(double value)
        {
            this.consumerOne.accept(value);
            this.consumerTwo.accept(value);
        }
    }

    private record ChainedIntConsumer(IntConsumer consumerOne, IntConsumer consumerTwo) implements IntConsumer
    {
        @Override
        public void accept(int value)
        {
            this.consumerOne.accept(value);
            this.consumerTwo.accept(value);
        }
    }

    public static class ButtonListenerIntModifier implements IButtonActionListener
    {
        protected final IntSupplier supplier;
        protected final IntConsumer consumer;
        protected final int modifierShift;
        protected final int modifierControl;
        protected final int modifierAlt;

        public ButtonListenerIntModifier(IntSupplier supplier, IntConsumer consumer)
        {
            this(supplier, consumer, 8, 1, 4);
        }

        public ButtonListenerIntModifier(IntSupplier supplier, IntConsumer consumer, int modifierShift, int modifierControl, int modifierAlt)
        {
            this.supplier = supplier;
            this.consumer = consumer;
            this.modifierShift = modifierShift;
            this.modifierControl = modifierControl;
            this.modifierAlt = modifierAlt;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            int amount = mouseButton == 1 ? -1 : 1;

            if (GuiBase.isShiftDown()) { amount *= this.modifierShift; }
            if (GuiBase.isCtrlDown())  { amount *= this.modifierControl; }
            if (GuiBase.isAltDown())   { amount *= this.modifierAlt; }

            this.consumer.accept(this.supplier.getAsInt() + amount);
        }
    }

    public static class ButtonListenerDoubleModifier implements IButtonActionListener
    {
        protected final DoubleSupplier supplier;
        protected final DoubleConsumer consumer;
        protected final int modifierShift;
        protected final int modifierControl;
        protected final int modifierAlt;

        public ButtonListenerDoubleModifier(DoubleSupplier supplier, DoubleConsumer consumer)
        {
            this(supplier, consumer, 8, 1, 4);
        }

        public ButtonListenerDoubleModifier(DoubleSupplier supplier, DoubleConsumer consumer, int modifierShift, int modifierControl, int modifierAlt)
        {
            this.supplier = supplier;
            this.consumer = consumer;
            this.modifierShift = modifierShift;
            this.modifierControl = modifierControl;
            this.modifierAlt = modifierAlt;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            int amount = mouseButton == 1 ? -1 : 1;

            if (GuiBase.isShiftDown()) { amount *= this.modifierShift; }
            if (GuiBase.isCtrlDown())  { amount *= this.modifierControl; }
            if (GuiBase.isAltDown())   { amount *= this.modifierAlt; }

            this.consumer.accept(this.supplier.getAsDouble() + amount);
        }
    }
}
