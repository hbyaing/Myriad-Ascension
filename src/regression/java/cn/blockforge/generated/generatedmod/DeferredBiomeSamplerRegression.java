package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.plants.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.LongFunction;

/** Deferred traversal must preserve the synchronous component, order and retry state. */
final class DeferredBiomeSamplerRegression {
    private static int checks;
    private static void check(boolean ok,String text){checks++;if(!ok)throw new AssertionError(text);}
    private static final class Queue implements Executor {
        final ArrayDeque<Runnable> jobs=new ArrayDeque<>();boolean reject;
        public void execute(Runnable job){if(reject)throw new RejectedExecutionException();jobs.addLast(job);}
        void finish(){
            Runnable job=jobs.removeFirst();Thread worker=new Thread(job,"projection-regression-worker");worker.setDaemon(true);worker.start();
            try{worker.join(5000);}catch(InterruptedException e){throw new AssertionError(e);}
            check(!worker.isAlive(),"Sampling did not finish");
        }
    }
    private static String biome(long cell){int x=ConnectedBiomeRegion.x(cell),z=ConnectedBiomeRegion.z(cell);return x>=0&&x<48&&z>=0&&z<48&&!(x==20&&z<42)?"land":"ocean";}
    static void run(){
        checks=0;var queue=new Queue();var sampler=new DeferredBiomeSampler(queue,256);
        Thread owner=Thread.currentThread();var supplied=new ArrayList<>(List.of(1L,2L));
        LongFunction<String> operation=k->{check(Thread.currentThread()!=owner,"Height work ran on the caller");return "cell"+k;};
        check(sampler.poll(1,()->supplied,operation)==null,"Unfinished sample appeared ready");supplied.clear();
        for(int i=0;i<100;i++)check(sampler.poll(1,List::of,operation)==null,"Pending call waited or returned an invented value");
        check(queue.jobs.size()==1,"Repeated polls queued more than one job");queue.finish();
        check(sampler.poll(2,List::of,operation).equals("cell2"),"Worker batch retained a mutable caller collection");
        queue.reject=true;check(sampler.poll(3,List::of,operation)==null,"Rejected batch did not defer");
        check(sampler.poll(1,List::of,operation).equals("cell1"),"Rejected work destroyed cached values");
        queue.reject=false;sampler.poll(3,List::of,operation);queue.finish();check(sampler.poll(3,List::of,operation).equals("cell3"),"Rejected sample could not retry");

        long seed=ConnectedBiomeRegion.pack(1,1),tile=ConnectedBiomeRegion.tile(seed);
        var expected=new HashSet<Long>();var examined=new HashSet<Long>();var frontier=new ArrayDeque<Long>();
        frontier.add(seed);examined.add(seed);var order=new ArrayList<Long>();
        while(!frontier.isEmpty())ConnectedBiomeRegion.step("land",expected,examined,frontier,k->{order.add(k);return biome(k);},128,Long.MAX_VALUE,tile);
        queue=new Queue();sampler=new DeferredBiomeSampler(queue,256);var actual=new HashSet<Long>();examined=new HashSet<>();frontier=new ArrayDeque<>();frontier.add(seed);examined.add(seed);
        var accepted=new ArrayList<Long>();final var deferred=sampler;final var path=frontier;
        LongFunction<String> sample=k->{String value=deferred.poll(k,()->path.stream().limit(16).toList(),DeferredBiomeSamplerRegression::biome);if(value!=null)accepted.add(k);return value;};
        var savedFrontier=List.copyOf(frontier);var savedExamined=Set.copyOf(examined);
        check(ConnectedBiomeRegion.step("land",actual,examined,frontier,sample,128,Long.MAX_VALUE,tile)==0,"Pending traversal counted a visit");
        check(List.copyOf(frontier).equals(savedFrontier)&&examined.equals(savedExamined)&&actual.isEmpty(),"Pending sample consumed traversal state");
        int rounds=0;
        while(!frontier.isEmpty()){
            if(!queue.jobs.isEmpty())queue.finish();
            ConnectedBiomeRegion.step("land",actual,examined,frontier,sample,128,Long.MAX_VALUE,tile);
            check(queue.jobs.size()<=1,"Traversal spawned an unbounded backlog");check(++rounds<10000,"Deferred traversal starved");
        }
        check(actual.equals(expected),"Deferred search changed connected biome membership");
        check(accepted.equals(order),"Deferred search changed canonical visit order or skipped a boundary");
        check(Collections.min(actual).equals(Collections.min(expected)),"Deferred search changed quota seed origin");
        check(actual.size()==48*48-42,"Fixture lost its connected passage around a biome barrier");
        System.out.println("Deferred biome regression: "+checks+" checks; "+actual.size()+" identical members and "+order.size()+" identical ordered visits");
    }
}
