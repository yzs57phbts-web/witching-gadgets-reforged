package witchinggadgets;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.inventory.ClickType;
import thaumcraft.aspect.*;
import thaumcraft.blockentity.ResearchTableBlockEntity;
import thaumcraft.menu.ResearchTableMenu;
import thaumcraft.network.*;
import thaumcraft.research.*;

/** Test driver: solves real generated hex puzzles using the same validated requests as the UI. */
final class SurvivalResearch {
    record State(HexGrid.Cell cell,Aspect aspect){}
    static void check(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    static Set<HexGrid.Cell> connected(Map<HexGrid.Cell,ResearchNote.Hex> board){
        var start=board.entrySet().stream().filter(e->e.getValue().type()==ResearchNote.Hex.GIVEN).findFirst().orElseThrow().getKey();
        var result=new HashSet<HexGrid.Cell>();var todo=new ArrayDeque<HexGrid.Cell>();todo.add(start);result.add(start);
        while(!todo.isEmpty()){var cell=todo.remove();var a=board.get(cell).aspect().orElseThrow();for(int d=0;d<6;d++){var n=cell.neighbour(d);var hex=board.get(n);if(hex!=null&&hex.aspect().isPresent()&&!result.contains(n)&&HexGrid.linked(a,hex.aspect().get())){result.add(n);todo.add(n);}}}return result;
    }
    static List<State> bridge(ResearchNote note){
        var board=HexGrid.index(note.hexes());var done=connected(board);var queue=new ArrayDeque<State>();var previous=new HashMap<State,State>();
        for(var cell:done){var state=new State(cell,board.get(cell).aspect().orElseThrow());queue.add(state);previous.put(state,null);}
        while(!queue.isEmpty()){
            var state=queue.remove();
            if(board.get(state.cell()).type()==ResearchNote.Hex.GIVEN&&!done.contains(state.cell())){
                var route=new ArrayList<State>();for(var step=state;step!=null;step=previous.get(step))route.add(step);Collections.reverse(route);return route;
            }
            for(int direction=0;direction<6;direction++){
                var cell=state.cell().neighbour(direction);var hex=board.get(cell);if(hex==null)continue;
                boolean repeated=false;for(var parent=state;parent!=null;parent=previous.get(parent))if(parent.cell().equals(cell)){repeated=true;break;}if(repeated)continue;
                for(var aspect:hex.aspect().isPresent()?List.of(hex.aspect().get()):Arrays.asList(Aspect.getEnabledAspects())){
                    var next=new State(cell,aspect);if(HexGrid.linked(state.aspect(),aspect)&&!previous.containsKey(next)){previous.put(next,state);queue.add(next);}
                }
            }
        }throw new IllegalStateException("No playable hex connection for "+note.key());
    }
    static int solve(ServerPlayer player,BlockPos pos){
        check(player.containerMenu instanceof ResearchTableMenu,"research table menu absent");var table=(ResearchTableBlockEntity)player.level().getBlockEntity(pos);int moves=0;
        while(table.getNote()!=null&&!table.getNote().complete()){
            check(moves<200,"hex solver exceeded move limit");var board=HexGrid.index(table.getNote().hexes());var path=bridge(table.getNote());boolean advanced=false;
            for(var step:path)if(board.get(step.cell()).type()==ResearchNote.Hex.EMPTY){
                ResearchTableBlockEntity.handlePlace(new AspectPlacePayload(pos,step.cell().q(),step.cell().r(),Optional.of(step.aspect())),player);moves++;advanced=true;
                check(table.getNote()!=null,"note vanished during solving");if(table.getNote().complete())break;
                var placed=HexGrid.index(table.getNote().hexes()).get(step.cell());check(placed.aspect().orElse(null)==step.aspect(),"native aspect placement rejected "+step);
            }
            check(advanced,"hex solver made no progress");
        }
        check(table.getNote()!=null&&table.getNote().complete(),"native table did not complete the puzzle");return moves;
    }
}
