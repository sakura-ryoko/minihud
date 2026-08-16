package fi.dy.masa.minihud.renderer.shapes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import com.google.gson.*;

import fi.dy.masa.malilib.util.data.json.JsonUtils;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.renderer.RenderContainer;

public class ShapeManager
{
    public static final String SHAPE_FILE_EXT = ".shape";
    public static final String SHAPE_FILE_DIR = "shapes";
    public static final ShapeManager INSTANCE = new ShapeManager();

    private final List<ShapeBase> shapes = new ArrayList<>();
    @Nullable private ShapeBase selectedShape;

    @Nullable
    public ShapeBase getSelectedShape()
    {
        return this.selectedShape;
    }

    public void setSelectedShape(@Nullable ShapeBase shape)
    {
        this.selectedShape = shape;
    }

    public List<ShapeBase> getAllShapes()
    {
        return this.shapes;
    }

    public void setAllNeedsUpdate()
    {
        for (ShapeBase shape : this.shapes)
        {
            shape.setNeedsUpdate();
        }
    }

    public void addShape(ShapeBase shape)
    {
		shape.onShapeInit();
        this.shapes.add(shape);

        RenderContainer.INSTANCE.addRenderer(shape);
    }

    @Nullable
    public static ShapeBase loadShapeFromFile(Path file)
    {
        try
        {
	        String contents = Files.readString(file);
            JsonElement element = JsonParser.parseString(contents);

            if (element.isJsonObject())
            {
                JsonObject o = element.getAsJsonObject();

                if (JsonUtils.hasString(o, "type"))
                {
                    ShapeType type = ShapeType.fromString(JsonUtils.getString(o, "type"));

                    if (type != null)
                    {
                        ShapeBase shape = type.createShape();
                        shape.fromJson(o);
                        return shape;
                    }
                }
            }
        }
        catch (IOException e)
        {
	        MiniHUD.LOGGER.error("ShapeManager#loadShapeFromFile: Exception reading file '{}'; {}", file.toString(), e.getLocalizedMessage());
        }

        return null;
    }

    public JsonElement exportShapeToJson(ShapeBase shape)
    {
        if (shape == null)
        {
            return new JsonObject();
        }

        return shape.toJson();
    }

    public boolean exportShapeToFile(ShapeBase shape, Path file, boolean overwrite)
    {
        boolean exists = Files.exists(file);

        if (exists && overwrite)
        {
            try
            {
                Files.delete(file);
            }
            catch (IOException e)
            {
                MiniHUD.LOGGER.error("ShapeManager#exportShapeToFile: Failed to delete file '{}'; {}", file.getFileName().toString(), e.getLocalizedMessage());
                return false;
            }
        }
        else if (exists)
        {
            MiniHUD.LOGGER.error("ShapeManager#exportShapeToFile: Failed; file '{}' already exists", file.getFileName().toString());
            return false;
        }

        JsonElement ele = this.exportShapeToJson(shape);

        if (ele.isJsonObject())
        {
            JsonObject o = ele.getAsJsonObject();

            if (o.isEmpty())
            {
                MiniHUD.LOGGER.error("ShapeManager#exportShapeToFile: Failed to export selected shape '{}' to JSON", shape.getDisplayName());
                return false;
            }

            try
            {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String json = gson.toJson(o);

                Files.writeString(file, json);
                MiniHUD.LOGGER.info("ShapeManager#exportShapeToFile: Exported '{}' shape to '{}'", shape.getDisplayName(), file.toString());
                return true;
            }
            catch (IOException e)
            {
                MiniHUD.LOGGER.error("ShapeManager#exportShapeToFile: Failed to write JSON file '{}': {}", file, e.getMessage());
                return false;
            }
        }

        return false;
    }

    public void removeShape(ShapeBase shape)
    {
        this.shapes.remove(shape);

        RenderContainer.INSTANCE.removeRenderer(shape);
    }

    public void clear()
    {
        for (ShapeBase shape : this.shapes)
        {
            RenderContainer.INSTANCE.removeRenderer(shape);
        }

        this.shapes.clear();
        this.selectedShape = null;
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();
        JsonArray arr = new JsonArray();
        int selected = -1;

        for (int i = 0; i < this.shapes.size(); ++i)
        {
            ShapeBase shape = this.shapes.get(i);
            arr.add(shape.toJson());

            if (this.selectedShape == shape)
            {
                selected = i;
            }
        }

        if (arr.size() > 0)
        {
            obj.add("shapes", arr);
        }

        if (selected != -1)
        {
            obj.add("selected", new JsonPrimitive(selected));
        }

        return obj;
    }

    public void fromJson(JsonObject obj)
    {
        this.clear();

        if (JsonUtils.hasArray(obj, "shapes"))
        {
            JsonArray arr = obj.get("shapes").getAsJsonArray();

            for (int i = 0; i < arr.size(); ++i)
            {
                JsonElement el = arr.get(i);

                if (el.isJsonObject())
                {
                    JsonObject o = el.getAsJsonObject();

                    if (JsonUtils.hasString(o, "type"))
                    {
                        ShapeType type = ShapeType.fromString(JsonUtils.getString(o, "type"));

                        if (type != null)
                        {
                            ShapeBase shape = type.createShape();
                            shape.fromJson(o);
                            this.addShape(shape);
                        }
                    }
                }
            }

            if (JsonUtils.hasInteger(obj, "selected"))
            {
                int selected = JsonUtils.getInteger(obj, "selected");

                if (selected >= 0 && selected < this.shapes.size())
                {
                    this.selectedShape = this.shapes.get(selected);
                }
            }
        }
    }
}
