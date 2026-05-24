GePIM 的实现可以理解为：

> **一个 Island Model（多岛遗传算法） + 一个基于 gene pattern 的 linkage learning 模块 + 一个只迁移 linked gene groups 的迁移算子 LGGM。**

它不是传统地迁移整个个体，而是先从多个 island 的优秀个体之间学习“哪些基因可能有关联”，然后迁移这些关联基因片段。核心创新点就是 **Linked Gene Groups Migration, LGGM**。

------

## 1. 总体框架

GePIM 的主流程仍然是标准 Island Model：

```text
it = 1

for each island:
    initialize(island)
    evaluate(island)

while not stopCondition:

    for each island:
        select(island)
        crossover(island)
        mutate(island)
        evaluate(island)

    if it % retrievalFreq == 0:
        retrieveLinkage()

    if it % migrationFreq == 0:
        migrateLinkedGeneGroups()

    for each island:
        evaluate(island)

    it = it + 1

return bestIndividual
```

也就是说，每个 island 内部先按普通 GA 进行演化，包括选择、交叉、变异和评价；然后周期性执行两个特殊操作：

1. `retrieveLinkage()`：提取 linkage 信息；
2. `migrateLinkedGeneGroups()`：迁移 linked gene groups。

------

## 2. Island 内部怎么演化

每个 island 是一个独立子种群。大部分时间，各 island 之间不直接交互，只在 migration 阶段交换信息。

Island 内部使用普通 GA：

```text
select()
uniform crossover()
bit-flip mutation()
evaluate()
```

文中明确说 GePIM 使用 **uniform crossover**，原因是它不依赖基因顺序；mutation 是逐基因检查，如果触发 mutation，就翻转该 bit。

所以 GePIM 的编码默认是 **binary encoding**，个体可以表示为：

```text
x = 0101011010...
```

------

## 3. Linkage 信息怎么表示

GePIM 不使用 DSM，也不使用 linkage tree，而是使用 **gene pattern**。

一个 gene pattern 是一个基因位置集合：

[
P={p_1,p_2,\ldots,p_l}
]

表示这些位置上的基因被认为可能存在依赖关系。

例如：

```text
gene pattern = {1, 4, 6}
```

表示第 1、4、6 个基因被认为是一组 linked genes。文中说明 gene pattern 是通过比较两个个体得到的：只保留两个个体中取值不同的位置。比如比较：

```text
individual A = 001100
individual B = 010101
```

不同的位置是第 2、3、6 位，所以得到：

```text
gene pattern = {2, 3, 6}
```

这个 pattern 的长度不是固定的。

------

## 4. `retrieveLinkage()` 怎么实现

这是 GePIM 的 linkage learning 阶段。

### 4.1 每个 island 提供两个优秀个体

对每个 island，取两个个体：

```text
currentBest_i：当前 island 种群里的最优个体
bestSoFar_i：该 island 历史上找到过的最优个体
```

这两个个体不一定相同。

如果有 (N) 个 island，那么一共会得到：

[
2N
]

个优秀个体。

### 4.2 两两比较优秀个体

把这 (2N) 个个体两两比较。每比较一对个体，就生成一个 gene pattern：

[
P(a,b)={j \mid a_j \neq b_j}
]

也就是两个优秀个体在基因上的差异位置集合。

文中的直觉是：如果两个个体都已经比较优秀，但它们在某些位置仍然不同，那么这些差异位置可能对应不同局部最优中的关键 building blocks，因此这些位置可能存在 linkage。

### 4.3 放入全局 gene pattern pool

所有生成的 gene pattern 会被放入一个全局池：

```text
genePatternPool
```

这个池有最大容量：

```text
genePatternPoolSize
```

如果池没满，就直接加入；如果池满了，新 pattern 会随机替换池中的一个旧 pattern。作者的理由是：当前无法准确判断哪个 linkage 有用，所以让经常被生成的 linkage 更容易留下来。

实现伪代码可以写成：

```cpp
void retrieveLinkage() {
    vector<Individual> elites;

    for (Island& island : islands) {
        elites.push_back(island.currentBest);
        elites.push_back(island.bestSoFar);
    }

    for (int a = 0; a < elites.size(); ++a) {
        for (int b = a + 1; b < elites.size(); ++b) {
            Pattern p;

            for (int g = 0; g < genotypeLength; ++g) {
                if (elites[a].gene[g] != elites[b].gene[g]) {
                    p.positions.push_back(g);
                }
            }

            if (!p.positions.empty()) {
                insertPatternIntoPool(p);
            }
        }
    }
}
```

------

## 5. `migrateLinkedGeneGroups()` 怎么实现

这是 GePIM 的核心迁移算子，叫 **Linked Gene Groups Migration, LGGM**。

传统 Island Model 迁移的是整个个体：

```text
island A sends individual x to island B
```

GePIM 迁移的是 gene pattern 标记的一组基因：

```text
island A sends genes at positions {p1, p2, ...} to island B
```

文中强调，迁移整个个体虽然能带来 building blocks，但这些 building blocks 后续可能被普通 crossover 破坏；所以 GePIM 直接迁移 linked gene groups。

### 5.1 迁移步骤

一次 LGGM 的过程是：

1. 选择两个 island；
2. 从两个 island 中各选出若干个 fitness 最好的个体；
3. 一个 island 作为 source island，另一个作为 receiving island；
4. source 个体和 receiving 个体按 fitness 排名配对；
5. 对每一对个体，从 `genePatternPool` 中随机选一个 gene pattern；
6. 把 source 个体中该 pattern 对应位置的基因复制到 receiving 个体中；
7. 重新 evaluate receiving island 中被修改的个体。

伪代码：

```cpp
void migrateLinkedGeneGroups() {
    Island& sourceIsland = selectSourceIsland();
    Island& recvIsland   = selectReceivingIsland();

    vector<Individual*> sources = sourceIsland.getBestIndividuals(migrationNum);
    vector<Individual*> receivers = recvIsland.getBestIndividuals(migrationNum);

    for (int i = 0; i < migrationNum; ++i) {
        Pattern p = genePatternPool.randomPattern();

        Individual* src = sources[i];
        Individual* dst = receivers[i];

        for (int pos : p.positions) {
            dst->gene[pos] = src->gene[pos];
        }

        evaluate(*dst);
    }
}
```

文中给的例子很直观：

```text
receiving individual = 01010101
source individual    = 10101010
gene pattern         = {1, 3, 5, 7}
```

迁移后，receiving individual 变成：

```text
11111111
```

因为只把 source 中第 1、3、5、7 位复制过去。

------

## 6. GePIM 的方法分类

文中把 GePIM 归类为一种 **linkage learning 方法**。

它的 linkage 特征是：

| 维度             | GePIM 的做法                        |
| ---------------- | ----------------------------------- |
| 方法框架         | Island Model                        |
| linkage 表示     | gene pattern                        |
| linkage 存储     | centralized，全局 gene pattern pool |
| linkage 生成方式 | evolution results comparison        |
| linkage 使用位置 | 只在 LGGM 迁移时使用                |
| 普通 crossover   | uniform crossover                   |
| 是否依赖基因顺序 | 不依赖                              |

文中明确说，GePIM 的 linkage information 只在 LGGM 中使用；普通 island 内部的 crossover 仍然是 uniform crossover。

------

## 7. 实验中的参数设置

文中调参后的 GePIM 参数如下：

| 参数                                    | 最终值                |
| --------------------------------------- | --------------------- |
| Crossover probability                   | 0.3                   |
| Population size per island              | 200                   |
| Number of islands                       | 30                    |
| Migration frequency                     | 50                    |
| Number of migrating individuals         | 40                    |
| Gene pattern pool size                  | 300                   |
| Linkage information retrieval frequency | 500                   |
| Mutation probability                    | (1/(3 \times length)) |

也就是说，实验里 **每 50 代执行一次 LGGM 迁移**，**每 500 代提取一次 linkage 信息**。作者还指出，调参后更频繁地交换 building blocks、较少地收集 linkage 信息，效果更好，因为较晚阶段的优秀个体更充分演化，提取到的 linkage 质量可能更高。

------

## 8. 一句话总结

GePIM 的实现逻辑是：

> **多个 island 各自用普通 GA 演化；周期性比较各 island 的优秀个体，生成 gene pattern 作为 linkage 信息；再通过 LGGM 只迁移这些 gene pattern 标记的基因组，而不是迁移整个个体。**

所以它的关键不是 crossover，而是：

```text
best individuals comparison → gene pattern pool → linked gene groups migration
```

这也是它和普通 Island Model 的主要区别。