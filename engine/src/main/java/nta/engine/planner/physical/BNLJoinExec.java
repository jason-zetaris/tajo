package nta.engine.planner.physical;

import nta.catalog.Schema;
import nta.catalog.SchemaUtil;
import nta.engine.SubqueryContext;
import nta.engine.exec.eval.EvalContext;
import nta.engine.exec.eval.EvalNode;
import nta.engine.planner.logical.JoinNode;
import nta.engine.utils.TupleUtil;
import nta.storage.FrameTuple;
import nta.storage.Tuple;
import nta.storage.VTuple;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BNLJoinExec extends PhysicalExec {
  private final SubqueryContext ctx;
  // from logical plan
  private Schema inSchema;
  private Schema outSchema;
  private EvalNode joinQual;
  private EvalContext qualCtx;

  // sub operations
  private PhysicalExec outer;
  private PhysicalExec inner;

  private JoinNode ann;

  private final List<Tuple> outerTupleSlots;
  private final List<Tuple> innerTupleSlots;
  private Iterator<Tuple> outerIterator;
  private Iterator<Tuple> innerIterator;

  private boolean innerEnd;
  private boolean outerEnd;

  // temporal tuples and states for nested loop join
  private FrameTuple frameTuple;
  private Tuple outerTuple = null;
  private Tuple outputTuple = null;
  private Tuple innext = null;

  private final int TUPLE_SLOT_SIZE = 10000;

  // projection
  private final int[] targetIds;

  public BNLJoinExec(SubqueryContext ctx, JoinNode ann, PhysicalExec outer,
      PhysicalExec inner) {
    this.ctx = ctx;
    this.outer = outer;
    this.inner = inner;
    this.inSchema = SchemaUtil.merge(outer.getSchema(), inner.getSchema());
    this.outSchema = ann.getOutputSchema();
    this.joinQual = ann.getJoinQual();
    this.qualCtx = this.joinQual.newContext();
    this.ann = ann;
    this.outerTupleSlots = new ArrayList<Tuple>(TUPLE_SLOT_SIZE);
    this.innerTupleSlots = new ArrayList<Tuple>(TUPLE_SLOT_SIZE);
    this.outerIterator = outerTupleSlots.iterator();
    this.innerIterator = innerTupleSlots.iterator();
    this.innerEnd = false;
    this.outerEnd = false;

    // for projection
    targetIds = TupleUtil.getTargetIds(inSchema, outSchema);

    // for join
    frameTuple = new FrameTuple();
    outputTuple = new VTuple(outSchema.getColumnNum());
  }

  public Tuple next() throws IOException {
    if (outerTupleSlots.isEmpty()) {
      for (int k = 0; k < TUPLE_SLOT_SIZE; k++) {
        Tuple t = outer.next();
        if (t == null) {
          outerEnd = true;
          break;
        }
        outerTupleSlots.add(t);
      }
      outerIterator = outerTupleSlots.iterator();
      outerTuple = outerIterator.next();
    }
    if (innerTupleSlots.isEmpty()) {
      for (int k = 0; k < TUPLE_SLOT_SIZE; k++) {
        Tuple t = inner.next();
        if (t == null) {
          innerEnd = true;
          break;
        }
        innerTupleSlots.add(t);
      }
      innerIterator = innerTupleSlots.iterator();
    }
    if((innext = inner.next()) == null){
      innerEnd = true;
    }
    while (!ctx.isStopped()) {
      if (!innerIterator.hasNext()) { // if inneriterator ended
        if (outerIterator.hasNext()) { // if outertupleslot remains
          outerTuple = outerIterator.next();
          innerIterator = innerTupleSlots.iterator();
        } else {
          if (innerEnd) {
            inner.rescan();
            innerEnd = false;
            
            if (outerEnd) {
              return null;
            }
            outerTupleSlots.clear();
            for (int k = 0; k < TUPLE_SLOT_SIZE; k++) {
              Tuple t = outer.next();
              if (t == null) {
                outerEnd = true;
                break;
              }
              outerTupleSlots.add(t);
            }
            if (outerTupleSlots.isEmpty()) {
              return null;
            }
            outerIterator = outerTupleSlots.iterator();
            outerTuple = outerIterator.next();
            
          } else {
            outerIterator = outerTupleSlots.iterator();
            outerTuple = outerIterator.next();
          }
          
          innerTupleSlots.clear();
          if (innext != null) {
            innerTupleSlots.add(innext);
            for (int k = 1; k < TUPLE_SLOT_SIZE; k++) { // fill inner
              Tuple t = inner.next();
              if (t == null) {
                innerEnd = true;
                break;
              }
              innerTupleSlots.add(t);
            }
          } else {
            for (int k = 0; k < TUPLE_SLOT_SIZE; k++) { // fill inner
              Tuple t = inner.next();
              if (t == null) {
                innerEnd = true;
                break;
              }
              innerTupleSlots.add(t);
            }
          }
          
          if ((innext = inner.next()) == null) {
            innerEnd = true;
          }
          innerIterator = innerTupleSlots.iterator();
        }
      }

      frameTuple.set(outerTuple, innerIterator.next());
      if (joinQual != null) {
        joinQual.eval(qualCtx, inSchema, frameTuple);
        if (joinQual.terminate(qualCtx).asBool()) {
          TupleUtil.project(frameTuple, outputTuple, targetIds);
          return outputTuple;
        }
      } else {
        TupleUtil.project(frameTuple, outputTuple, targetIds);
        return outputTuple;
      }
    }

    return null;
  }

  @Override
  public Schema getSchema() {
    return outSchema;
  }

  @Override
  public void rescan() throws IOException {
    outer.rescan();
    inner.rescan();
    innerEnd = false;
    innerTupleSlots.clear();
    outerTupleSlots.clear();
    innerIterator = innerTupleSlots.iterator();
    outerIterator = outerTupleSlots.iterator();
  }
}
