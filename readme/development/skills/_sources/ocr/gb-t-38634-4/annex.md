# 附录与参考文献

> PDF pages 116–127 (approx; refine after TOC QA)

## Page 116

GB/T 38634.4一2020
表 C.18 (HE)
aa exema] ewe | wnwma [a[o[e[incomme| wm |e |

C.2.4.9.3 全定义-使用路径测试覆盖

根据6.3.7.5 提供的公式和上述导出的测试覆盖项，

MBX, eezum =e X 100% =100%

PAU st HP ie a AF a HY EGS BT 100% ,
C.2.4.9.4 第5步:汇集测试集(TD5)

全使用测试导出的测试用例覆盖了所有的测试竹盖项,将其作为识别测试集的例子。 所有 Is
Complex 为FALSE 测试用例组合成一个测试集,为TRUE 的测试用例组合成另一个测试集:

TSHL: 测试用例 1.

TS2; 测试用例 2，
C.2.4.9.5 第6步:导出测试规程(TD6)

所有测试集按照执行的顺序组合成一个测试规程,具体如下:

TPs 按照测试集指定的顺序覆盖 TS1 中的所有测试用例,青著盖 TS2 中的所有测试用例。

110

## Page 117

GB/T 38634.4一2020
附 R D
《资料性附录)
基于经验的测试设计技术的应用指南和示例

D.1 基于经验的测试设计技术的使用规则和示例

本附录为5.4 和6.4 的要求提供了指南。本附录展示了在一个例子中运用基于经验的测试设计技
术的情况。例子符合 GB/T 38634.2 定义的测试设计和实施过程。
D2 ”基于经验的测试设计技术示例
D.2.1 错误猜测
D.2.1.1 介绍

错误猜测的目的是导出材盖了可能错误的测试用例集,使用测试大员在此前测试项中的知识和经
验。导出满足每种错误类型的测试用例* 由测试大员确定当前的测试项可能存在的错误类型。该技术
通常是在其他基于规格说明的测试设计技术5如等价类划分和边界值分析)后使用,用于补充这些技术
的目标错误类型。
D.2.1.2 ”规格说明

考虑汕试项一一generate_grtding,测试依据如下，

组件收到一个考试成绩(不高于 75 分)输入和一个课程作业(cyw)成绩(不高于 25 分)输入,根据
输入输出一个“A"到“D"”的课程等级。等级是通过计算考试和课程作业成绩的总和得到的,具体如下:
大于或等于70                       —*A?

大于或等于50,小于70          —*B?
大于或等于30,小于 50               ——~*¢?
小于 30                              一
在检测到无效输入(例如分数超出了预期的范围)时生成一个出错信息人6*FM7)表示。所有的输入
都用整数。
D.2.1.3 第1步,识别特征集(TD1)
因为测试依据中只有一个测试项,只需要定义一个特征:
FS1:generate_grading 功能
D.2.1.4 第2步:导出测试条件CTD2)
基于过去其他测试项中类似错误的知识和经验导出可能存在的错误类型列表,并以此导出测试条
件. 对于 generate_grading 功能,可以导出如下测试条件;
TCONDI; 输入NULL                                                             (FS1)
TCOND2; 输入0                                                                   (FS1》
TCOND3: 输入负数                                                           (FS1)
TCOND4: 反向输入                                                    (FS1)
it

## Page 118

GB/T 38634.4一2020

TCOND5: 输入非常大的数字(例如10 位)                                           (FSI)

TCONDS : 输入非常长的字符串(例如10 个字符)                                    (FS1)
D.2.1.5 第3步:导出测试覆盖项(TD3)

缺陷的一般类型是测试覆盖项(即测试茸盖项和测试条件一致)。因此,可以定义下面的测试蓝

盖项:

TCOVER1: 输入NULL L                                                    (TCONDI1》

TCOVER2: 输入0                                                            ¢TCOND2)

TCOVERS: 输大负数                                                         CTCOND3》

TCOVER4: 反向输入                                                 (TCOND§)

TCOVERS : 输入非常大的数字(例如 10 位)                                    (TCONDS)

TCOVERS: 输入非常长的字符串(例如 10 个字符)                             (TCOND6)
D.2.1.6 第4步,导出测试用例(TD4)

测试用例可依据不同的缺陷类型导出;根据选择的缺陷类型确定满足测试用例参数的输入,确定预
期结果 ,重复上述步骤查到测试用例包括了所有测试团盖项。对于本例,导出测试用例如表-D.1一
# D.4。
表 D.1 错误猜测的测试用例(一)
im (CO
se fom
aaa fe
TCOVERI       TCOVERI       TCOVERI            TCOVER?
femme eee we ee
表 D.2 错误猜测的测试用例(二)
ii
CSC CC
xm pe
aa
测试黎盖项             TCOVER2    TCOVER2        TCOVERS    TCOVERS
DORE MRR ORISA | RE    课程作业成线
om |
112

## Page 119

GB/T 38634.4—2020
表 D.3 ”错误猜测测试用例(三)
Pre rr         TCOVERS        TCOVER4        TCOVERS    TCOVERS
SAYERS 。 |考试和课程作业成绩 |考试成绩      课程作业成绩
表 D.4 错误猜测测试用例(四)
abe a            TCOVERS             TCOVERS    TCOVERG    TCOVERG
AG AK ALE Me RR    AG ik    课程作业成绩 |考试和谍程作业成绩
D.2.1.7 第5步:汇集测试集(TD5)
所有测试用例都是无效的,均会出现出错消息,.可归大一个测试集.如于所示:
TS1 :测试用例集1.2,3:4.5.6,7,8.9,10,11,12,13.14.15,16。
D.2.1.8 第6步:导出测试规程(TD6)
只需要一个规程,如下所示:
TPI:按顺序覆盖TS1 中测试用鲍。
D.2.1.9 错误猜测的测试覆盖率
如 6.4.1 所述,尚无错误猪测覆盖率的计算方法。
113

## Page 120

GB/T 38634.4一2020
附 录 E
【资料性附录)
可交换的测试设计技术的应用指南和示例
E.1 可交换的测试设计技术的应用指南和示例
虽然本部分将技术分为基于结构.基于规格说明或基于经验的;实际上一些技术可以交换使用
《见 5.1》。下面的例子说明了分支测试(通常是指基于结构的技术)如何应用于基于规格说明的测试，
证明了技术可以交换使用。
E.2 分支测试作为以规格说明为基础的技术
F.2.1 规格说明
考虑下面的例子规格说明 ,定义了一个登录功能,需要输大用户各和密码来判断用户是否有效:
组件要求输入用户名和密码。 用户必须输入正确的用户名和相应的密码才能登录系统。 用户有3
次输入用户各和密码的机会,每次不能超过 20 s。如果用户输入用户名和密码错误 3 次或者超过20 sy
则会锁定系统,不准许有任何进一步的登录尝试。
组件的控制流图如图 E,1 所示。
时间>208OR次数-
B6
OR次数-3
获取用户名 |                ences
FAP AR                   aA
AND次数<3                  AND次数<3
AND @f fa] S20 s                AND时间拓20s
图 E.1 登录功能的控制流图
E.2.2 第1步,识别特征集(TD1)
测试基准中只定义了一个测试项,只需要定义一个特征集;
FS1:登录功能
E.2.3 第2步:导出测试条件(TD2)
控制流图提供了很好的功能概述,帮助识别分支测试的测试条件。控制流程图中的方块表示组件
中的程序源代码段,菱形代表判定。萎形指向判定结果。可能的控制转移是:
Bl1-~B2     B3>B4     B5>B4
B2>B3     B3->B6     BS B6
114

## Page 121

GB/T 38634.4一2020
B3->B2      Bi-BS      BSB?

XE Ty SE «WN ESE eC) ,在控制流图用箭头表示。共有 9 UF Pas
TCOND1;B1>B2                                                                                                (FS1)
TCOND2:B2->B3                                                         (FS1)
TCOND3:B3->B2                                                         (FS1)
TCOND4:B3-=>Bi                                                          (FS1)
TCONDS : B3-*B6                                                         (FS1)
TCOND6 : B4-*B5                                                                                                            (FS1)
TCOND7:B5>B4                                                          (FS1)
TCONDS ; B5>B6                                                         (FS1)
TCONDS ;B5->B7                                        (FS1)

E.2.4 第3步:导出测试覆盖项(TD3)
对于分支覆善,测试覆盖项是控制流图中的分支,与测试条件相同。在本例中,分支覆盖有 9 个测
试覆盖项,基体如下 :
TCOVER1:B1-=B2                                                                                      (TCOND1)
TCOVER2: B2->B3                                                   (TCOND2)
TCOVERS: B3->B2                                                   (TCONDS3)
TCOVER4:B3->B4                                                    (TCOND4)
TCOVERS ; B3->B6                                                   (TCONDS)
TCOVERG: BA B5                                                   (TCOND6)
TCOVER7:B5-B4                                                    (TCOND?)
TCOVERS: B5->B6                                                   (TCONDS8)
TCOVER9:B5-~B7                                                   (TCOND9)
E.2.5 第4步:导出测试用例(TD4)
分支测试通过识别控制流子路径导出测试用例,子路径是测试过程中没有执行的一个或多个分
支(测试覆盖项) ,确定满足子路径的输大,确定每个测试的预期结果,重复上述步骤直到达到要求的
测试蓝盖率。对于分支镍盖,任何单独的测试用例满足一个子路径,因此可能会有许多潜在的判定
和分支。
考虑执行子路径 B1一B2-=*B3-Bd4一B5-=B7 的测试用例。，导出测试用例:第一次输入有效的
用户名和对应的密码。 路径执行了5 个分支中的 5 个分支,实现了 56儿的覆盖率(和判定覆盖不
同)。
BETES IT FBR Bl B2-> B38 B2> B3+ BA > BS + BA > BS BS 的测试用例。生成子路
径:用户名无效.用户名有效但密码错误,用户名有效但正确的密码输入时间超过 20 秒。现在除了
B3—>B6 以外的所有分支均被覆盖。该子路径可以被覆商,例如,输入三次无效的用户名或者等待输
和信有效的用户过长时间。现在覆盖了所有的分支,包括所有判定。需要注意的是,一些判定条件没
有覆盖。

著盖所有控制流子路径的测试用例如表 E.1 所示。

115

## Page 122

GB/T 38634.4—2020
RE) 登录功能的测试用例
测试用例                 阴户名                  密码等            子路径            Mik A         预期绪果
re aeal = [S
TCOVERI.
TCOVER?,
Andy }<20 — |Warhol  |<20        eo TCOVERI.              登录
TCOVER6，
TGOVER9
TCOVERI.
TCOVER?.
ee es              cr           一| Bi-二B3-B2-B3|TCOVER3，
Ne         TinaVAlib |<20 +B B5-+ Bi BS|TCOVERI.            Riki
Andy      =0
Warhol |=21        一本                    TCOVERS.
TCOVER?.
TCOVERS
TCOVERI.
Brandy | =21                                 Bi--B2-+B3+B6 |TCOVER2，
TCOVERS
E.2.6 第5步:汇集测试集(TD5)
只需要三个测试用例,可以选择这三个测试用例组合成一个测试集;
TS1:测试用例 E.2 和 3。
E.2.7 第6步:导出测试规程(TD6)
只有三个测试用例和一个测试集,可以选择只定义一个测试规程,
TP1:按照测试集的指定顺序蓝盖 TS1 中的所有测试用例。
E.2.8 ”分支测试覆盖率
根据 6.3.2 提供的公式和上述导出的测试覆盖项计算覆盖率;
WR Riges =2 x 100% =100%
PALM REF 4p Se ae HA EE IY EAHA 100% |
116

## Page 123

GB/T 38634.4一2020
附 录 下
《资料性附录)
测试设计技术著盖有效性
本部分未提供测试设计技术或测试完成准则(也称为测试充分准则?选择的指南,分别从第5 章和
第章中选择测试设计技术或测试完成准则。没有指南的主要原因是:哪些技术和准则是最有效的,尚
无公认的共识。唯一的共识是测试设计技术或测试完成准则选择要有所变化,它依赖于许多因素,如风
险;临界性,应用领域和成本。对测试用例设计和测量技术的研究到目前为止没有明确的结果,下面提
供了一些理论性的结果,这些结果没有考虚成本 -
没有要求选搓相应的测试用例设计和测试覆盖测量方法。基于规格说明的测试设计技术对检测遗
漏错误是有效的,而基于结构的测试设计技术只能检测已知的错误。所以测试计划通常使用边界值分
析导出初始的测试用例集 ,同时要求达到 100儿的分支材普率。不同的方法可能会导致分支测试导出
的测试用例用以补充边界值分析测试用例集遗漏的分支。
理想的情况下 ,选择测试完成准则的测试萎盖率应尽可能达到 100冯。对测试覆盖级别的严格定
义测试蓝盖率有时会导致该测试禾盖级别不具有可操作性,然而,本部分的第6 章定义了在计算测试蓝
盖率时 ,允许只考虑可行的履盖项,这样就可以达到 100% FR AY A
100% CELALAT 1005%%)的测试完成准则可能按颗序水及一些准则.其中准则显式包含了其他准则。
一个准则包含了另一个准则 ,对于所有测试用例及其测试依据和测试套件,如果满足第一个准则 ,则也
满足第二准则。例如,分支覆盖包含语句覆盖,因为如果达到了分支材盖率(100锥)则也会达到 100 %
的语句团盖率。
注意这里描述的“包含"关系和测试攻羡准则严格关联(而不是测试设计技术,所以只提供测试设
计技术相对有效性的间接指标。
并非所有的测试覆盖准则都通过包含排序相关联,基于规格说明和基于结构的准则完全不相关
的。准则的含序关系适用于基于结构的测试设计技术,如图 F.1 所示,从一个准则指向另一准则表
示第一准则包含第二准则。当一个测试团盖准则没有出现在偏序关系中,则说明和其他准则没有包
BRR.
尽管很直观,但在使用包含关系选择测试完成准则前需要考虑受到的限制
一一首先,只和可用的测试完成准则的子集相关,在该子集中没有提供有效性的指示,因此仍需要
考虑上图中没有显示的其他准则。
一一其次,一个准则包含另一个准则的包含关系没有提供对数量的测量,因此没有提供相对成本效
益的措施，
一一第三,偏序只适用于单个准则,如果推荐使用多个准则,则至少有一个功能性准则和一个结构
性准则 。
-一一最后,也是最重要的,包含关系不一定根据测试完成准则发现故障(测试效率)对其排序。例
如,100六的路径材盖(当达到时?对于某些测试项可能比其包含的准则有效率,如关注数据流
的准则。这是因为一些错误对数据敏感,简单执行有错误的路径不能发现错误,但需要变量取
特殊值(例如,对整数变量“无保护的"除法可能会错误地包含在一个测试项中,只有当该变量
取负值时才会失败)。数据流谁则可以集中测试测试项行为的这些方面,从而增加了发现这类
错误的概率。在一些情况下 ,通过测试特定准则所需要的路径子集提高测试的有效性 ,用更多
的测试用例满足该子集，
包含关系高度依赖于全覆盖准则的定义,虽然上图符合第 5 童中的定义,但可能不适用于其他地方
使用的替代定义。
117

## Page 124

GB/T 38634.4—2020
Cm)                       :
C=)                       =
BEA
AU HITT
ACU -—#it MOM,
ADU ——~ 428 OB EE
APU INL
BD SKE
BC PSR PRM
BCC SRAM
MCDC ——#6iE RPE MR:
Ss ARM,
图F.1 结构测试覆盖准则的偏序关系
118

## Page 125

GB/T 38634.4一2020

附 录 G

(资料性附录)

测试设计技术对照
本附录描述了本部分和 GB/T 15532一2008 的测试设计技术的映射关系。
表 G.1 本部分与 GB/AT 15532一2008 测试设计技术映射

[san [me | anne [RM
i
ee
ee Oc
at | eae

Po saa [ee |
Psa [kana | Asaso [aan
ET
i
[sao [mmm | nae [a
ee

119

## Page 126

GB/T 38634.4一2020
参考 x wt

C1] GB/T 25000,10--2016 ”系统与软件工程 ”系统与软件质量要求和评价(SQuaRE) 第 10 部
分:系统与软件质量模型

[2 ISO/IEC TR 19759 Software Engineering一Guide to the Software Engineering Body of
Knowledge (SWEBOK)

[3] ISO/IEC 25030 Software engincering—Software product Quality Requirements and Evalua-
tion (SQuaRE)—Quality requirements

[4] BS 7925-2:1998 Software testing—-Software component testing

[5] BATH, G. and McKAY J., 2008, The Software Test Engineer’ s Handbook, 0’ Reilly
Media. Ine.

[6] BEIZER, B.. 1995, Black Box Testing. Techniques for Functional Testing of Software and
Systems. John Wiley &- Sons Inc.

[7] Non-Functional Testing. British Computer Society Special Interest Group in Software Tes-
tings [September 2011].

[8] BURNSTEIN, 1. 2003. Practical Software Testing: A Process—Oriented Approach,
Springer— Verlag.

[9] COPELAND. L.. 2004, A Practitioner’s Guide to Software Test Design. Artech
House. Ine,

[10] CHO, C. K.. 1987, Quality Programming. Wiley.

f11] CHOW, T. S.. 1978. Testing Software Design Modelled by Finite一State Machines. In:
IEEE Transactions on Software Engineering. Vol. SE-4(3).

[12] CRAIG. R, and JASKIEL. S.,. 2002. Systematic Software Testing. Artech House Inc.

[13] DESIKAN, S. and RAMESH. G.. 2007. Software Testing: Principles and Practices.
Pearson Education,

f14] GRINDAL, M.. OFFUTT, J. and ANDLER, S., 2005. Combination Testing Strategies:
A Survey. In; Software Testing. Verification and Reliability. John Wiley & Sons Ltd., 15. pp.
167-199,

[15] GROCHTMANN, M, and GRIMM, Grimm. K.. 1993. Classification Trees for Partition
Testing. In; Software Testing, Verification & Reliability, Wiley. 3(2), pp. 63-82.

[16] HASS,. A, M, Jonassen, 2008, Guide to Advanced Software Testing. Artech House.

[17] KANER, C., 1998. Testing Computer Software. TAB Books Inc.

[18] KERNIGHAN, B. W, and RICHIE, D. M., 1998, The C Programming Language. Pren-
tice Hall Software Series.

[19] KIT, E., 1995, Software Testing in the Real World; Improving the Process. ACM Press,

[20] MANDL, R.. 1985. Orthogonal Latin Squares; An Application of Experiment Design to
Compiler Testing. In Communications of the ACM, 28(10), pp. 1054-1058.

[21] MYERS, G., 1979. The Art of Software Testing. John Wiley & Sons Inc.

[22] NURSIMULU, K. and PROBERT, R. L.. 1995. Cause一Effect Graphing Analysis and
Validation of Requirements. In Proceedings of CASCON1995,

120

## Page 127

GB/T 38634.4一2020
[23] RTCA7DO-178C，Software Considerations in Airborne Systems and Equipment Certifica-
tion. RTCA, Ine, 2011,
[24] REID. S,. 1996. Popular Misconceptions in Module Testing, In Proceedings of the Soft-
ware Testing Conference (STC), Washington DC.
