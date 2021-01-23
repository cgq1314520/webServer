package com.server.pageCache;

import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;

/**
 * @author cgq
 * @version 2021.1.1-1
 * @apiNote  此部分程序的功能为定义页面缓存的哈希表结构，也即数组+链表的功能
 *           包括对于哈希表长度的定义和每个哈希表元素中哈希桶最大长度的限制
 */
public class HashBucketCache {
    //定义哈希表缓存结构的数组的长度
    public static final int length=128;
    //定义哈希表每一个数组元素中链表的最大长度为5，当长度大于5之后就会发生页面替换
    public static final int listLength=5;
    //哈希表的长度为128,也即给定的hash表中最大有128个key
    public HashTab[] hashTab=new HashTab[length];
    public HashBucketCache(){
        for (int i = 0; i < length; i++) {
            hashTab[i]= new HashTab(null);
        }
    }
    //哈希桶结构
    public static class HashPair{
        //文件的名字,类似于hashMap中的 key：value中的key
        public String name;
        //文件对应的内容，类似于hashMap中的 key：value中的value
        public byte[] content;
        //文件内容是content中数组中的前多少个
        public int length;
        //当前对应文件使用的次数(替换算法替换时的依据),初始时为0
        public int count=0;
        //指向下一个具有相同key的哈希桶元素
        public HashPair next;
        //构造函数
        public HashPair(String name,byte[] content,int length){
            this.name=name;
            this.content=content;
            this.length=length;
            this.next=null;
        }
    }
    //哈希表结构
    public static class HashTab{
        //当前hash表项中哈希桶的个数，初始时为0
        public int number=0;
        //哈希表中的哈希桶的头
        public HashPair head;
        //构造函数
        public HashTab(HashPair head){
            this.head=head;
        }
    }
    /**
     * 根据给定的key计算其hash值，从而间接决定该文件存放在哈希结构数组中的第几个位置上
     * 以下计算hash值和hashmap中计算hash值的方法类似（就是抄人家的）
     * @param key 键值，注意，这儿的key值在调用hash之前便能保证不为null
     * @return 计算得到的hash值
     */
    public static int hash(String key){
        int h;
        return (h = key.hashCode()) ^ (h >>> 16);
    }

    /**
     * 判断给定的fileName代表的文件是否已经存在于哈希表中（据此我们便可以直接通过判断决定是否读文件了）
     * @param fileName 要判断的文件的名字
     * @return 返回值代表是否已经缓存,如果已经缓存，则返回缓存的文件内容，否则返回null
     */
    public  HashPair getFileCacheByName(String fileName){
        //计算hash值
        int hashNum=hash(fileName);
        //得到文件可能存在的位置
        int i=(length-1)&hashNum;
        //在当前的位置上循环，判断文件是否存在
        if(hashTab[i].number==0){  //则说明没有缓存
            return null;
        }
        //程序执行到这里说明有元素，现在循环判断链表中是否有该问价你的缓存
        HashPair pair=hashTab[i].head;
        while(pair!=null){
            if(pair.name.equals(fileName)){ //说明文件存在，则返回缓存
                System.out.println("文件从缓存中得到");
                return pair;
            }
            pair=pair.next;
        }
        //程序执行到这里，则说明文件没有缓存
        return null;
    }
    /**
     * 给该hash缓存模型的数组中插入元素，元素是文件名字为key，文件内容为值
     * @param fileName 要存入缓存中到的文件名，在调用put之前就要确保filename不为null
     * @param content  要存入缓存中的文件内容
     * @param Con_length content的前多少个为文件的内容
     */
    public  void put(String fileName,byte []content,int Con_length){
        putVal(hash(fileName),fileName,content,Con_length);
    }

    /**
     * 根据给定的hash值将键值对存储到该页面缓存结构数组中的合适位置,由于要改变链表，所以应该是原子性的
     * @param hash key计算得到的hash值
     * @param key  要存入缓存中的文件的名字
     * @param content 要存入缓存中的文件的内容
     * @param Con_length content的前多少个为文件的内容
     */
    public synchronized  void putVal(int hash,String key,byte []content,int Con_length){
        HashPair p;
        int i=(length - 1) & hash;
        //通过(length-1)&hash值来确定该内容具体放在哈希表数组中的第几个位置
        //如果初始时为null，则说明该位置还没有使用过，所以直接执行下面的语句即可
        if ((p = hashTab[i].head) == null)
        {
            System.out.println("将"+key+"文件放入了缓存");
            //创建一个哈希桶用来存储具体的文件内容和数据
            HashPair node= new HashPair(key, content, Con_length);
            //说明当前元素使用了一次
            node.count=1;
            //将此个哈希桶放到哈希表位置i的第一个位置
            hashTab[i]= new HashTab(node);
            //说明位置为i处哈希桶的总个数为1
            hashTab[i].number=1;
        }
        else if(hashTab[i].number<listLength){
            //如果当前的位置不为null且桶的个数小于5，则循环判断当前要缓存的文件是否存在，
            // 如果存在则只该表所在位置hashPair的文件的访问次数，否则将该文件的缓存放到末尾
            //并将桶的个数加1，文件的访问次数加1
            //第一步：判断当前的文件是否存在，如果存在，则访问次数加1，且将其移动到链表的末尾（为了方便LRU的进行）代表刚刚访问，此时头结点代表最近最少使用的页面
            HashPair pair=hashTab[i].head;
            HashPair pre = null;
            while(pair!=null){
                //判断当前结点是否是要缓存的文件,如果是,则访问次数加1(这儿代表文件已经缓存了)
                if(pair.name.equals(key)){
                    System.out.println("缓存中有这个文件");
                    pair.count+=1;
                    //将pair最终要放到链表末尾，为了使得LRU成立，所以在这儿需要注意
                    if(pre!=null){
                        pre.next=pair.next;  //也即前一个结点的指向发生了变化
                    }
                    else{//这个说明是头结点是放置缓存文件的结点，所以要进行以下处理
                        hashTab[i].head=pair.next;
                    }
                    break;  //此时跳出循环时，则pair不为null，所以可以据此判断文件是否存在
                }
                pre=pair;
                pair=pair.next;
            }
            //判断pair是否为空，从而判断文件是否已经缓存了
            if(pair==null){  //如果为null，说明原本没有缓存,所以插入一个新的结点，并将桶的个数加一，这个结点本身就在链表的末端，所以也符合LRU规则
                System.out.println("将"+key+"文件放入缓存");
                HashPair now= new HashPair(key, content, Con_length);
                now.count=1;  //当前文件的访问次数为1
                pre.next=now; //让新插入的结点在末尾
                hashTab[i].number+=1;   //让该位置中桶的个数加1
            } else {
                //pair不为null时，while中已经改变了访问次数了，现在为了满足LRU的替换规则，所以我们应该将刚刚访问过的该页面替换到链表的末尾
                //第二步：把pair结点移动到链表末尾
                while(pre.next==null){
                    pre.next=pair;
                    pair.next=null;
                    pre=pre.next;
                }
            }
        }
        else {
            //如果当前的位置不为null且桶的个数大于等于5，则循环判断当前要缓存的文件是否存在，
            // 如果存在则只该表所在位置hashPair的文件的访问次数，否则就必须要根据给定的规则
            //进行页面替换才行了，具体的替换规则有LRU(最近最少使用)、LFU(最不经常使用)等等
            //第一步：先判断当前的文件是否存在，如果存在，则访问次数加1，且将其移动到链表的末尾（为了方便LRU的进行）
            HashPair pair=hashTab[i].head;
            HashPair pre = null;
            while(pair!=null){
                //判断当前结点是否是要缓存的文件,如果是,则访问次数加1
                if(pair.name.equals(key)){
                    System.out.println("页面已经缓存");
                    pair.count+=1;
                    //将pair最终要放到链表末尾，为了使得LRU成立，所以在这儿需要注意
                    if(pre!=null){
                        pre.next=pair.next;  //也即前一个结点的指向发生了变化
                    }
                    else{//这个说明是头结点是放置缓存文件的结点，所以要进行以下处理
                        hashTab[i].head=pair.next;
                    }
                    break;  //此时跳出循环时，则pair不为null，所以可以据此判断文件是否存在
                }
                pre=pair;
                pair=pair.next;
            }
            //判断pair是否为空，从而判断文件是否已经缓存了
            if(pair==null){  //如果为null，说明原本没有缓存,所以应该插入一个新的结点，但是当前情况下，由于桶的个数已经达到了上限5，所以要进行页面替换
                //进行页面替换了，依据不同的替换规则，则有不同的替换方法
                //第一种：LRU替换，直接替换掉当前所在哈希表项链表的头部即可
                System.out.println("进行了LRU页面替换");
                LRU(i,key,content);
            } else {
                //pair不为null时，说明已经缓存上了，由于while中已经改变了访问次数了，现在为了满足LRU的替换规则，所以我们应该将刚刚访问过的该页面替换到链表的末尾
                //第二步：把pair结点移动到链表末尾
                while(pre.next!=null){
                    pre=pre.next;
                }
                pre.next=pair;
                pair.next=null;
            }
        }
    }

    /**
     * LRU（最近未使用替换）页面替换算法
     * @param i 替换哈希表数组中的第几项
     * @param key 替换后的文件名
     * @param content 替换后的文件内容
     */
    private  void LRU(int i, String key, byte[] content) {
        hashTab[i].head.name=key;
        hashTab[i].head.content=content;
        hashTab[i].head.count=1;
    }
}
