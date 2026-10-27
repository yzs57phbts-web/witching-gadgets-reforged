package witchinggadgets;
import com.google.gson.*;
import net.minecraft.core.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.world.level.block.Block;
/** The render and selection volumes share the original cuboids, including all six growth stages. */
public final class RoseGeometry {
    static JsonObject DATA;
    static JsonObject data(){if(DATA==null)try(var in=RoseGeometry.class.getResourceAsStream("/assets/witchinggadgets/port/rose_geometry.json")){DATA=JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}return DATA;}
    static int stage(BlockGetter l,BlockPos p){return l.getBlockEntity(p) instanceof MachineEntity m?Math.clamp(m.progress,0,5):5;}
    static boolean attached(BlockGetter l,BlockPos p,Direction d){var at=p.relative(d);var s=l.getBlockState(at);return !s.is(Content.block("rose_vines"))&&s.isFaceSturdy(l,at,d.getOpposite());}
    static java.util.List<Direction> faces(BlockGetter l,BlockPos p){var out=new java.util.ArrayList<Direction>();for(var d:Direction.values())if(d!=Direction.UP&&attached(l,p,d))out.add(d);if(out.isEmpty())out.add(Direction.DOWN);return out;}
    private static final VoxelShape[][] CACHE=new VoxelShape[6][64];
    /** Selection needs each vine face's outline, not a new 3D boolean mesh of every twig. */
    static VoxelShape shape(BlockGetter l,BlockPos p){int stage=stage(l,p),mask=0;for(var d:faces(l,p))mask|=1<<d.ordinal();synchronized(CACHE){var cached=CACHE[stage][mask];if(cached!=null)return cached;VoxelShape shape=Shapes.empty();for(var d:Direction.values())if((mask&(1<<d.ordinal()))!=0){double x0=16,y0=16,z0=16,x1=0,y1=0,z1=0;for(var e:data().getAsJsonObject(d==Direction.DOWN?"floor":"wall").getAsJsonArray(""+stage)){var a=e.getAsJsonArray();x0=Math.min(x0,a.get(0).getAsDouble());y0=Math.min(y0,a.get(1).getAsDouble());z0=Math.min(z0,a.get(2).getAsDouble());x1=Math.max(x1,a.get(3).getAsDouble());y1=Math.max(y1,a.get(4).getAsDouble());z1=Math.max(z1,a.get(5).getAsDouble());}shape=Shapes.or(shape,switch(d){case SOUTH->Block.box(16-x1,y0,16-z1,16-x0,y1,16-z0);case WEST->Block.box(z0,y0,16-x1,z1,y1,16-x0);case EAST->Block.box(16-z1,y0,x0,16-z0,y1,x1);default->Block.box(x0,y0,z0,x1,y1,z1);});}return CACHE[stage][mask]=shape;}}
}
