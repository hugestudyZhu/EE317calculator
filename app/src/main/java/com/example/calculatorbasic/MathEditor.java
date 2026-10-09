package com.example.calculatorbasic;

import java.io.Serializable;
import java.util.*;

/** Structured calculator document. Cursor positions belong to slots, not source-code characters. */
public final class MathEditor implements Serializable {
    private static final long serialVersionUID=1L;
    public enum Kind { ATOM, GROUP, FRACTION, POWER, ROOT, FUNCTION }
    public static final class Row implements Serializable {
        private static final long serialVersionUID=1L;
        public final List<Element> elements=new ArrayList<>();
        public String source(){StringBuilder b=new StringBuilder();for(Element e:elements)b.append(e.source());return b.toString();}
    }
    public static final class Element implements Serializable {
        private static final long serialVersionUID=1L;
        public final Kind kind; public final String text; public final List<Row> slots;
        Element(Kind kind,String text,Row... slots){this.kind=kind;this.text=text;this.slots=new ArrayList<>(Arrays.asList(slots));}
        public String source(){
            switch(kind){
                case ATOM:return text;
                case GROUP:return "("+slots.get(0).source()+")";
                case FRACTION:return "frac("+slots.get(0).source()+","+slots.get(1).source()+")";
                case POWER:return "("+slots.get(0).source()+")^("+slots.get(1).source()+")";
                case ROOT:return text.equals("root")?"root("+slots.get(1).source()+","+slots.get(0).source()+")":text+"("+slots.get(0).source()+")";
                default:List<String> args=new ArrayList<>();for(Row r:slots)args.add(r.source());return text+"("+String.join(",",args)+")";
            }
        }
    }
    public static final class Stop {
        public final Row row;public final int index;
        Stop(Row row,int index){this.row=row;this.index=index;}
    }
    public Row root=new Row(); public Row active=root; public int position;
    public String source(){return root.source();}
    public MathEditor copy(){
        Map<Row,Row> rows=new IdentityHashMap<>();MathEditor e=new MathEditor();
        e.root=copyRow(root,rows);e.active=rows.get(active);e.position=position;return e;
    }
    private static Row copyRow(Row source,Map<Row,Row> rows){
        Row target=new Row();rows.put(source,target);
        for(Element element:source.elements){
            Row[] slots=new Row[element.slots.size()];
            for(int i=0;i<slots.length;i++)slots[i]=copyRow(element.slots.get(i),rows);
            target.elements.add(new Element(element.kind,element.text,slots));
        }
        return target;
    }
    public static MathEditor fromSource(String source){MathEditor e=new MathEditor();try{e.root=new Importer(source).parse();}catch(RuntimeException ex){e.root=literal(source);}e.active=e.root;e.position=e.root.elements.size();return e;}
    private static Row literal(String text){Row row=new Row();for(int i=0;i<text.length();i++)row.elements.add(atom(text.substring(i,i+1)));return row;}
    private static Element atom(String text){return new Element(Kind.ATOM,text);}
    private void add(Element e){active.elements.add(position++,e);}
    public void insertSource(String source){
        if(source==null||source.isEmpty())return;
        if(source.startsWith("*")||source.startsWith("/")||source.startsWith("+")){insert(source.substring(0,1));source=source.substring(1);}
        Element e=new Element(Kind.GROUP,"",fromSource(source).root);multiplyIfNeeded(e);add(e);
    }
    public void insert(String token){
        if(token==null||token.isEmpty())return;
        if(token.equals("NEG")){add(atom("-"));return;}
        if(token.equals(",")||token.equals(";")||token.equals("; ")){nextSlot();return;}
        if(token.equals("(")){Element e=new Element(Kind.GROUP,"",new Row());multiplyIfNeeded(e);add(e);active=e.slots.get(0);position=0;return;}
        if(token.equals(")")){exitSlot();return;}
        if(token.equals("10^(")||token.equals("10^")){if(position>0&&operand(active.elements.get(position-1)))add(atom("*"));insert("10");power("");return;}
        if(token.startsWith("^")||token.equals("*10^")){
            if(token.equals("*10^")){insert("*");insert("1");insert("0");power("");}
            else {String exponent=token.substring(1);if(exponent.startsWith("(")&&exponent.endsWith(")"))exponent=exponent.substring(1,exponent.length()-1);power(exponent);}return;
        }
        if(token.startsWith("frac(")){fraction();return;}
        int open=token.indexOf('(');
        if(open>0){String name=token.substring(0,open);int count=1;for(int i=open;i<token.length();i++)if(token.charAt(i)==',')count++;
            if(name.equals("rand"))count=0;
            if(name.equals("root"))count=2;
            Row[] slots=new Row[count];for(int i=0;i<count;i++)slots[i]=new Row();
            Kind kind=name.equals("sqrt")||name.equals("cbrt")||name.equals("root")?Kind.ROOT:Kind.FUNCTION;
            Element e=new Element(kind,name,slots);multiplyIfNeeded(e);add(e);if(count>0){active=slots[0];position=0;}return;
        }
        if(token.length()>1&&token.matches("[0-9.]+")){for(int i=0;i<token.length();i++)insert(token.substring(i,i+1));return;}
        if(token.matches("[+*/-]")&&position>0){Element previous=active.elements.get(position-1);if(previous.kind==Kind.ATOM&&previous.text.matches("[+*/-]")&&position>1)active.elements.remove(--position);}
        Element e=atom(token);multiplyIfNeeded(e);add(e);
    }
    private boolean operand(Element e){return e.kind!=Kind.ATOM||e.text.matches("[A-Za-z0-9.]+|π|[!%]");}
    private void multiplyIfNeeded(Element next){
        if(position==0||!operand(next)||next.text.equals("!")||next.text.equals("%"))return;Element last=active.elements.get(position-1);if(!operand(last))return;
        boolean nextDigit=next.kind==Kind.ATOM&&next.text.matches("[0-9.]");
        boolean lastDigit=last.kind==Kind.ATOM&&last.text.matches("[0-9.]");
        if(nextDigit&&lastDigit)return;
        add(atom("*"));
    }
    private Row takeOperand(){
        Row r=new Row();if(position==0)return r;
        Element last=active.elements.get(position-1);if(!operand(last))return r;
        int end=position;
        while(end>0){Element e=active.elements.get(end-1);if(e.kind!=Kind.ATOM||!e.text.matches("[!%]"))break;end--;}
        if(end==0||!operand(active.elements.get(end-1)))return r;
        int start=end-1;
        Element core=active.elements.get(start);
        if(core.kind==Kind.ATOM&&core.text.matches("[0-9.]")){
            int first=start;while(first>0){Element prior=active.elements.get(first-1);if(prior.kind!=Kind.ATOM||!prior.text.matches("[0-9.eE+-]"))break;first--;}
            StringBuilder number=new StringBuilder();for(int i=first;i<end;i++)number.append(active.elements.get(i).text);
            java.util.regex.Matcher match=java.util.regex.Pattern.compile("(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?$").matcher(number);
            if(match.find())start=first+match.start();
        }
        r.elements.addAll(active.elements.subList(start,position));active.elements.subList(start,position).clear();position=start;return r;
    }
    public void fraction(){Row n=takeOperand(),d=new Row();Element e=new Element(Kind.FRACTION,"",n,d);add(e);active=n.elements.isEmpty()?n:d;position=active.elements.size();}
    private void power(String exponent){Row b=takeOperand(),p=literal(exponent);Element e=new Element(Kind.POWER,"",b,p);add(e);if(b.elements.isEmpty()){active=b;position=0;}else if(exponent.isEmpty()){active=p;position=0;}}
    private void collect(Row row,List<Stop> stops){stops.add(new Stop(row,0));for(int i=0;i<row.elements.size();i++){for(Row slot:row.elements.get(i).slots)collect(slot,stops);stops.add(new Stop(row,i+1));}}
    public List<Stop> stops(){List<Stop> stops=new ArrayList<>();collect(root,stops);return stops;}
    public void move(int direction){List<Stop> list=stops();for(int i=0;i<list.size();i++)if(list.get(i).row==active&&list.get(i).index==position){Stop next=list.get(Math.max(0,Math.min(list.size()-1,i+direction)));active=next.row;position=next.index;return;}}
    private static final class Parent {final Row row;final int index;final Element element;Parent(Row row,int index,Element e){this.row=row;this.index=index;element=e;}}
    private Parent parent(Row row,Row target){for(int i=0;i<row.elements.size();i++){Element e=row.elements.get(i);for(Row slot:e.slots){if(slot==target)return new Parent(row,i,e);Parent found=parent(slot,target);if(found!=null)return found;}}return null;}
    public void vertical(int direction){Parent p=parent(root,active);if(p==null)return;int slot=p.element.slots.indexOf(active),next;
        if(p.element.kind==Kind.POWER)next=direction<0?1:0;else next=Math.max(0,Math.min(p.element.slots.size()-1,slot+direction));
        active=p.element.slots.get(next);position=Math.min(position,active.elements.size());
    }
    public void nextSlot(){Parent p=parent(root,active);if(p==null)return;int next=p.element.slots.indexOf(active)+1;if(next<p.element.slots.size()){active=p.element.slots.get(next);position=0;}else{active=p.row;position=p.index+1;}}
    public void exitSlot(){Parent p=parent(root,active);if(p!=null){active=p.row;position=p.index+1;}}
    public void delete(){
        if(position>0){Element e=active.elements.get(position-1);if(!e.slots.isEmpty()){active=e.slots.get(e.slots.size()-1);position=active.elements.size();}else active.elements.remove(--position);return;}
        Parent p=parent(root,active);if(p==null)return;
        boolean empty=true;for(Row slot:p.element.slots)if(!slot.elements.isEmpty())empty=false;
        if(empty){p.row.elements.remove(p.index);active=p.row;position=p.index;}else move(-1);
    }
    public boolean incomplete(){return emptySlots(root);}
    private boolean emptySlots(Row row){for(Element e:row.elements)for(Row slot:e.slots)if(slot.elements.isEmpty()||emptySlots(slot))return true;return false;}
    public void select(Row row,int index){for(Stop s:stops())if(s.row==row&&s.index==index){active=row;position=index;return;}}
    /** Import precedence-aware source into editable slots, preserving its value. */
    private static final class Importer {
        final String s;int p,depth;
        Importer(String s){this.s=s==null?"":s;}
        Row parse(){Row row=expression();space();if(p!=s.length())throw new IllegalArgumentException();return row;}
        void space(){while(p<s.length()&&Character.isWhitespace(s.charAt(p)))p++;}
        boolean take(char ch){space();if(p<s.length()&&s.charAt(p)==ch){p++;return true;}return false;}
        Row expression(){Row row=term();while(true){if(take('+')){row.elements.add(atom("+"));row.elements.addAll(term().elements);}else if(take('-')||take('−')){row.elements.add(atom("-"));row.elements.addAll(term().elements);}else return row;}}
        Row term(){Row row=unary();while(true){if(take('*')||take('×')){row.elements.add(atom("*"));row.elements.addAll(unary().elements);}else if(take('/')||take('÷')){row=one(new Element(Kind.FRACTION,"",row,unary()));}else{space();if(p<s.length()&&(s.charAt(p)=='('||Character.isLetter(s.charAt(p)))){row.elements.add(atom("*"));row.elements.addAll(unary().elements);}else return row;}}}
        Row unary(){if(++depth>60)throw new IllegalArgumentException();Row row;if(take('-')||take('−')){row=one(atom("-"));row.elements.addAll(unary().elements);}else if(take('+'))row=unary();else row=power();depth--;return row;}
        Row power(){Row row=primary();while(true){if(take('!'))row.elements.add(atom("!"));else if(take('%'))row.elements.add(atom("%"));else break;}if(take('^'))row=one(new Element(Kind.POWER,"",unwrap(row),unwrap(unary())));return row;}
        Row unwrap(Row row){while(row.elements.size()==1&&row.elements.get(0).kind==Kind.GROUP)row=row.elements.get(0).slots.get(0);return row;}
        Row primary(){space();if(take('(')){Row row=expression();if(!take(')'))throw new IllegalArgumentException();return one(new Element(Kind.GROUP,"",row));}
            if(p>=s.length()||s.charAt(p)==','||s.charAt(p)==')')return new Row();
            int start=p;char ch=s.charAt(p);
            if(Character.isDigit(ch)||ch=='.'){
                while(p<s.length()&&(Character.isDigit(s.charAt(p))||s.charAt(p)=='.'))p++;
                if(p<s.length()&&(s.charAt(p)=='e'||s.charAt(p)=='E')){int q=p+1;if(q<s.length()&&(s.charAt(q)=='+'||s.charAt(q)=='-'))q++;int digits=q;while(q<s.length()&&Character.isDigit(s.charAt(q)))q++;if(q>digits)p=q;}
                String number=s.substring(start,p);int exponent=Math.max(number.indexOf('e'),number.indexOf('E'));
                if(exponent>=0){Row scientific=literal(number.substring(0,exponent));scientific.elements.add(atom("*"));scientific.elements.add(new Element(Kind.POWER,"",literal("10"),literal(number.substring(exponent+1))));return one(new Element(Kind.GROUP,"",scientific));}
                return literal(number);
            }
            if(!Character.isLetter(ch))throw new IllegalArgumentException();while(p<s.length()&&(Character.isLetter(s.charAt(p))||s.charAt(p)=='_'))p++;String name=s.substring(start,p);
            if(!take('('))return one(atom(name));
            List<Row> args=new ArrayList<>();if(!take(')')){do{args.add(expression());}while(take(','));if(!take(')'))throw new IllegalArgumentException();}
            if(name.equals("frac")&&args.size()==2)return one(new Element(Kind.FRACTION,"",args.get(0),args.get(1)));
            if(name.equals("root")&&args.size()==2)return one(new Element(Kind.ROOT,name,args.get(1),args.get(0)));
            return one(new Element((name.equals("sqrt")||name.equals("cbrt"))&&args.size()==1?Kind.ROOT:Kind.FUNCTION,name,args.toArray(new Row[0])));
        }
        Row one(Element e){Row row=new Row();row.elements.add(e);return row;}
    }
}
