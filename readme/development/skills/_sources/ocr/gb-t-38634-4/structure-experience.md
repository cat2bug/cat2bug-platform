# 基于结构/经验的技术

> PDF pages 81–115 (approx; refine after TOC QA)

## Page 81

GB/T 38634.4—2020

覆盖率,wywgaa = 4.x 100% =100%

ee weere ry =< x 100% = 100%

WH cy woun = jo % 100% =100%

因此,对于单步转移测试,全转移测试和?2 步转移,测试覆盖项的覆盖率都达到了 100%
B.2.9 ”场景测试
B.2.9.1 介绍

场景测试的目的是根据选择的覆益程度导出材葡测试项场景的测试用例。 场景测试是以测试依据
的分析为基础,以动作序列的形式产生测试项行为的模型,该动作序列组成了贯穿测试项的工作流，

下面说明了两种类型的场景测试。 第一个例子是基于场苔测试的一种通用模式,第二个是基于用
例的具体例子。
B.2.9.2 例1
B.2.9.2.1 规格说明

考虑测试项 withdraw_eash*是自动取款机(ATM)系统的一部分,测试依据如王所示:

withdraw_cash 功能允许有银行账户的客户通过 ATM 从他科的账户取款。取款要求用户有一个
激活的账户.一张有效的卡和匹配的密码以及一台可工作的 ATM。当取款完成后,赚户余额扣除取款
金额,打印取款回执条.ATM 可用于为下一位用户服务，

根据用户的需求定义下面的场景:

基本场景

一一成功从账户取款

可选场景

不支持取款,因为:

一一银行让不能被 ATM 识别,被拒绝;

一一用户输入密码错误不多于2次;

一一用户输入密码错误 3 次,ATM fF ER;

一一用户选择存款或者转账,不选择取款;

一一用户选择了错误账户;此账户在插大的卡中不存在

一一用户输大的取款金矣是无效的:

一一ATM 中现金不足;

一一用户输入不符合面额的取款金额;

一一用户输入的取款金额是超过了每日报大取款金额;

一一用户银行账户中的金额不足。

注: 实际上;也可能存在代表其他情况的场时,例如用户在取款过程中的某个点上选择取消。
B.2.9.2.2 “步骤 1:识别特征集(TD1)

在测试依据中只有一个测试项,只需要定义一个特征集:

FS1: withdraw_cash

75

## Page 82

GB/T 38634.4一2020
B.2.9.2.3 步骤2:导出测试条件(TD2)

为了识别测试条件,需要生成测试项的模型以识别每个场景中的场景(和场景中的活动)。图 B.15
例子中的模型是一个事件流模型,在这些符号中*主"路径用粗黑线表示 ,工作流的开始和结束点进行
了标记,每个动帮用唯一的标识符来表示,代表用户(U)或系统CS)(即测试项?的动作。

Ea
人 人DR
SL2         U3.2   TL. 4.2
   =
:
密码错误      调用存款
<3       Pd d
本
$2.3
| |
tt} 加
合适的金额 发放现金     FTES  we
te
UB           用户取卡
ca
 °
‘Sb. 496
us
a
$5. Sallis
每日到现   U5
S5. KP
图 B.15 ”取款功能场景流程图
76

## Page 83

GB/T 38634.4一2020

在场景测试中,测试条件是需要在测试中覆盖的基本场景和可选场景(即用户和系统交互的事件流

用序列组成一个场景)。在规格说明中描述了 11 个场景,包括 工个基本场景和 10 个可选场景。这些场
景即测试条件(覆盖 FSD ,如下所示:

TCONPD1 :到款成功                                       (覆盖-Ul1,.S1.1,U2,.S2.1,U3.1，
Ud4,S4.1,U5,S5.1,S6,.S7,S8，
$9.U6)

TCOND2:银行卡不能被 ATM 识别                           (WL U1.S1.2,S9.U6)

TCONDS :用户输入密码错误次数小于3 次                   (覆盖 U1.S1.1.U2.S82.2)

TCOND4:用户输入密码错误次数等于 3 次                 (覆盖Ul,SL1,U2,S2.2,U2,S2.2，
U2,S2.3,S3)

TCONDS :用户选择存款或者转账                               (覆盖 U1,.S1.1,U2,82.1.U3.2,
S10)

TCONDS ; FHP ce FE TR MK                               (覆盖 U1,S1.1,U2.S2.1.U3.1.
U4.S4.2)

TCOND? :用户输入无效的取款金额                          (#8 U1,S1.1,U2,$2.1.U3.1.
U4, S4.1.U5,$5.2)

TCONDS8; ATM 中现念不足                               (覆盖:01,S1.1,0U2,.S2.1,U3.1，
U4,S4.1.U5,$5.3)

TCOND9:用户输入不符合面额的取款金额                  (覆盖.U1.S1.1,U2,S2.1,U3.1;
U4.S4.1,U5.S5.4)

TCONPD10:用户输入的取款金矣超过了每日最大的金额 HEU S11, 2.82.1. 3,1.
U4,S4.1,U5,S5.5)

TCOND11:用户银行三户中的金额不足                      (覆盖 Ul,S1L1.U2,.S2.1,U3.1，
U4,S4.1.U5,S5.6)

B.2.9.2.4 步骤3:导出测试覆盖项(TD3)
在场景测试中,测试覆六项和测试条件一样 ,都是基本场景和可选场景。

TCOVER1I=TCOND1                      TCOVER7=TCOND?

TCOVER2= TCOND2                      TCOVERS = TCONDS

TCOVER3= TCOND3                      TCOVERS = TCOND9

TCOVER4 = TCOND4                      TCOVERI10=TCOND1O

TCOVERS = TCONDS                                  TCOVERI1=TCONDI1

TCOVER6 = TCOND6

B.2.9.2.5 ”步骤4:导出测试用例(TD4)

选定要帮盖的一个场景来导出测试用例,确定测试用例的输大场景,确定测试的预期结果,重复上
述步骤直到覆盖所有要求的场景。测试用例的步骤通常用自然语言的格式说明。如果假设所确定每一
个测试覆盖项都需要一个测试用例,可以导出表:B.36一表 B.46 的测试用例，

注 1, 对于百个测试用例,用于填充每个输大字段可以有很多种变化.可以用等价类划分生成十充每个输大字段的

一组值。

77

## Page 84

GB/T 38634.4一2020
RB.36 场景测试的测试用例(一)
wa
RBI7 场景测试的测试用例{二)
a
a
Coe ce ee ee
RBIS ”场景测试的测试用例(三)
mw
ws
78

## Page 85

GB/T 38634.4一2020

表 B.39 场景测试的测试用例(四)
a
ei
ee ee
预期结果         长,消息提示卡已经被春,用户需要联系银行重新激活。ATM 保存用户的银行来:ATM 是

激活的,可操作的,等待搬人一张客户银行卡

表 B.40 场景测试的测试用例(五)

a
mA
RBA 场景测试的测试用例(六)
[ID
wa

79

## Page 86

GB/T 38634.4—2020

表 B.42 场景测试的测试用例(七)
wh |
ws

RBA ”场景测试的测试用例(八)
mw
ws

表 B.44 场景测试的测试用例(九)
一
wa

80

## Page 87

GB/T 38634.4一2020
表 B.44 (4)
IW RE              BIE RIAL Ad ITA RR YE ATM,
预期结果               系统鱼示信息提示输大的会额不符侣 ATM 余铬的面额,提示用户重新输入金额
表 B.45 ”场景测试的测试用例(十
BRAS          再户输大的取款金额超过了每日最天的金额
ET
有效客户三疡的有效银行卡一一般设 293910982246 JEAT RAY
正确密码一一般设 5672 是正确的且和第行卡匹配
ah           ATM ~~ ¥ 100.ATM 只有下50 纸币
用户同户余额一一于3 000
用户每日最大取款金额一一至1 000
WAS w—— ¥2 000
RAR | REL — i A — aR A Lk YEE ATM
预期结果               系统显示信息提示输入的金额超出了每日最大的取款金奖,提示用户重新输入金额
mann
表 B.46 场景测试的测试用例(十一)
mia
HY Fe    用户银行三疡中的金额不足
0
有效客户账户的存效银行卡一一假设 293910982246 是有效的
正确密码一一般设 5672 是正确的且和银行卡匹配
aA
FLP WK J Ae tal ——¥ 20
Ma & BI-—~ ¥ 50
RE               HRB BER AL PG A A SAT RA a 9 J — EA. ATM
预期结果               系统显示信息提示用户银行三户中的金后不足,提示用户重新输入金寡
CE
注 2: 为了可读性,上面的测试用鲍都在独立的表中显示,实际上,也可挫在一个表中显示。
B.2.9.2.6 ”步骤5:汇集测试集(TD5》
根据是否材盖基本场景和可选场景将测试分组:
81

## Page 88

GB/T 38634.4—2020
TS1:测试用例 1.
TS2:测试用例 2,3,41,5,6,7,8,.9,10,11。
B.2.9.2.7 “步骤6:导出测试规程(TD6)
要求的两个测试过程如下所示:
TPL: Wie TS 中的所有测试用人鲁,以测试集指定的顺序。
TP2:查盖 TS2 中的所有测试用例,以测试集指定的顺序。
B.2.9.28 ”场景测试覆盖率
根据 6.2.9 提供的公式和上述导出的测试获盖项:
WRK iceman atx 100% =100%
PA He pS EH AK Ha ET LE TA] 100%
B.2.9.3 例2
B.2.9.3.1 介绍
用例测试是场景测试的一种形式;测试用例的导出是基于测试项的用例模型。下面将用一个单独
的例子,给本标准的用户提供一个该测试技术的完整可用示例。
B.2.9.3.2 ”规格说明
考虑如表 BAT 示例中*change_password"测试项的用例。
RBA 密码修改用例的例子
一wa
一sasnea
-一wa
ee
一ww
82

## Page 89

GB/T 38634.4—2020
3 B.47 (HE)
| 34 | 用六和町入错误的当前密码
EEC
        系统错误信息提示“当前密码输人错误,请重试",并且定位到
"       当前密码输入和杠
(a | 用六输入新密码且长度少于8位
| .43 |  系统鲁误信息提示“新密码长度至少需要 8 位,请重试"
| | 用户验大新密码和当前密码一致
| sa 系统链误信息提示“新侯码必须不同于当前密码,请重试”
|  用户重新输大新密码.和步蒜4 中输入的新密码不一臻
| 三 系统错误信息提示"新密码不一孤,请重试”
Ee
新密码必须和当前密码不加
新密码长度至少有8位
系统用星号(* )码示所有当前密码和新密码
新用户登录系统时首次使用
一般丢个用户每年平均使用两次
用户可以随时点击*修改密码"按钮
注: 实际上,也可能存在代表其他情况的场景,例如用户输入无效形式的新密码-
B.2.9.3.3 步骤 1:识别特征集(TD1)
在测试依据中只有一个测试项,只需要定义一个特征集;
FS1;change_password 功能
B.2.9.3.4 “步骤 2:导出测试条件(TD2)
测试条件是当前用例中的基本场景和可选场景，在本例中,测试条件是:
TCONDI]: ARATE                             (FS1)
TCOND2; 可选流一一当前密码输入错误            (FS1)
TCOND3: 可选流一一新密码长度少于8 位        《FS1)
TCOND4: 可选流一一新密码和当前密码一臻         (FS1)
TCONDS; 可选流一一两次输入的新密码不一臻       (FS1)
B.2.9.3.5 步骤 3:导出测试覆盖项(TD3》
用例测试中的测试材盖项是基本场景和可选场景,如下所未:
&3

## Page 90

GB/T 38634.4一2020
TCOVER1: 基本流                     (TCOND1)
TCOVER2: 可选流一一当前密码输入错误        (TCOND2)
TCOVERS; 可选流一一新密码长度少于8 位       CTCOND3)
TCOVERS; 可选流一一新密码和当前密码一致     (TCOND4)
TCOVERS; 可选流一一两次输入的新密码不一致    (TCONDS)
B.2.9.3.6 “步骤 4:导出测试用例(TD4)
通过著盖一个场景来导出测试用例,确定输大来检验测试用例材闭的上路径,确定测试的预期结果，
重复上述步骤坦到覆盖所有要求的场景,相关的测试用例见表 B.48一表 HB.52。

表了.48 ”用例测试的测试用例(一)
pa |
a
2 | 当前人RSC |
Ps fmm ARMM | mia
ree | ARO MR |
[8 mrt OK | Rm |

表 B.49 用例测试的测试用例(二)

[ak作 | mpessai |
IC
|
a ee) |
ET国民IC
[ewe ACR |
| OK NRA |
4

## Page 91

GB/T 38634.4一2020
表 B.50 ”用例测试的测试用例(三)
Cr |
[man is —SSSCSCSCS~S~CSSS
IT
ee
rsxaansasm (| was i
有ITTTTTIR meRNNH RR
SEN CT ec
表 B.51 用例测试的测试用例(四)
me
i
aa |
er | wwmmascs |
ec | Was |
Er COC ee
表 8B.52 用例测试的测试用例(五)
85

## Page 92

GB/T 38634.4一2020
表 B.52 (4k)
a
ee
er | WE
用户重新输入新密冯,但和砂黄 3 中输大的                              、

EEC
BE ECO Ca Cone ae
B.2.9.3.7 ”步骤5:汇集测试集(TD5)

根据是否覆症基本路径和可选路径将测试分组;

TS1:测试用例 1

TS2:测试用例2,3.4,5
B.2.9.3.8 步骤6:导出测试规程(TD6)

要求的两个测试规程如下所示:

TPL: BEM TS) 中的所有测试用例,以测试集指定的顺序。

TP2:覆盖TS2 中的所有测试用例,以测试集指定的项序。

B.2.9.3.9 场景测试团盖率
根据 6.2.9 中提供的计算场景调试覆盖率的公式和上述导出的测试覆盖项;
WRK ems) 一己 100% =100%

B.2.10 ”随机测试

B.2.10.1 介绍

随机测试的目的是在选定的输入分布内生成测试项的输入参数,形成一个测试集,。本技术不需要
对测试项的输入域进行划分*仅要求输入值是从输大域中随机选择的。

B.2.10.2 ”规格说明

考虑测试项变换坐标,下面是测试依据;

组件需要将屏幕中用直角坐标系(z,y)表示的位置变为用极坐标系(>万)表示 ,替换公式为:7一
sdqrt(zs十ya)veos H 一工人rs。 直角坐标系的原点和极坐标系的极点都应该在屏幕的中心,工 轴是极坐
标系逆时针方向的起始线。 所有输入和输出都被表示为一个有范围和精度的定点数,如下所示:

输入

一一范围为一320一 十320 ,精度为 172

y-一-范围为一240一 十240,精度为 1727

输出

r一一范围为0一4100,精度为 1725

86

## Page 93

GB/T 38634.4一2020

五一一范围为 0~ (2 * pi) 1/2") 精度为了7号
B.2.10.3 步骤 1:识别特征集(TD1)

在测试依据中只有一个测试项,只需要省义一个特征集;

FS1:变换坐标功能
B.2.10.4 步骤 2:导出测试条件(TD2)

随机测试中的测试条件是为每个输入参数选择所有可能的输大值,具体如下 :

TCOND1: 一一范围为一320~ +320. 441 1/2°         (FS1)
TCOND2: yy一一范围为一240一 +240. EW 1/2"         (FS1)

B.2.10.5 步骤3:导出测试覆盖项(TD3)

在随机测试中没有明显的测试覆盖项。
B.2.10.6 ”步骤 4:导出测试用例(TD4)

构造测试用例首先选择输大分布区间 ,然后在每个测试条件上使用输入区问 ,最后确定每个测试用
例的预期结果(两个输出参数“和”“末”的“输出?如表 B.53 所示)。因为例子中测试项的输入参数的操
作耸布没有任何可用的信息,所以选抒一个均匀分布。从定义可知,可以随机选择 41024 PI C641 x
2中的任意一个,同时wy 可以随机选择 61568 个值(481X27)中的任意一个。如果使用预期的操作分
布而不是均匀分布,则需要注意。预期的分布会忽略部分输人域,可能导致一些意想不到的错误没有进
行测试。

因为每个测试用例应同时包含测试条件忠和>? 的一个随机测试输入值,所以每个测试用例履盖
T x Aly 两个测试条件。在均匀分布中,xz 和> 定义范围内的所有输入值有相同的概率被选为测试用
例的输入值。表 B.53 是四个测试用例。

表 B.53 ”随机测试的测试用例
ee 4 |
|

TCONDI        TCONDI        TCONDIE        TCONDI

ei AEWA ma ty) ET本
ai A WARCCM—w | ainonn | atosea | oamanty [ior
B.2.10.7 ”步骤5:汇集测试集(TD5)

假设所有的测试必须按照测试用例表中的顺序手动执行,可以定义一个测试集,如王所示:

TS1:手动测试一一测试用例 1,2,3,.4。
B.2.10.8 步骤 6:导出测试规程(TD6)

只需要执行一个测试集,则可以定义一个测试规程.如下所示:

87

## Page 94

GB/T 38634.4—2020
TP1:按照测试集中指定的顺序手动测试,覆盖所有 TS1 ER
B.2.10.9 ”随机测试测试率
如 6.2.10 中所述,目前尚无达成业界共识的方法用于计算随机测试测试覆盖项的覆盖率。
B.2.10.10 ”自动化随机测试
随机测试可以手动测试或自动化测试。完全自动化随机测试不需要大工干预,是最有效率的。但
基,为了达到完全自动化,应满足:
一一自动生成随机测试的输和值;
一一从测试依据中自动生成预期结果3或
一一自动按测试依据核对测试结果。
只要测试项的输入是已经定义的,使用伪随机数发生器自动生成随机测试的输入值并不难， 如果
用伤随机数发生器自动生成随机测试的输入值,这些值就不需要进行明确记录,因为可以重新生成相同
的值。如果使用一个已经记录的“种子”填充伪随机数发生器,这就是可能的。
自动生成预期结果和自动核对输出是比较难的。通常情况下,根据测试依据自动生成预期结果和
自动核对输出是不容易操作的,但是对于一些测试项是有可能的.例如:
一一可以使用热行与测试项相同功能的.可信和的.独立制作的软件(很可能不符合相同的约束,例如
处理速度.实现语言等六
一一仅检查测试项是否崩误(所以其预期结果是“不崩溃”7
一一测试项输出结果的性质使得核对结果相对容易。排序函数就是这样一个例子,检查输出是知
进行了正确的排序是非常简单的任务;
一一从每个输出生成输和是比较简单的(使用测试项的逆向功能)。例如,平方根函数可以通过对
输出进行平方逆向计算出本数的输入。
在B.2.10.2 中的例子中 ,玲标转换测试项可以用逆向功能函数进行自动检验。在这个例子中,从测
试项的测试依据中可以直接获得 reos甩一六。经过分析,可以推导出 rsinH =y. WAGER PASE
理的数值误差内,则测试项正确转换了坐标系。
尽管完全自动化随机测试是不切实际的,得是仍需要考虑,因为它在设计测试用例的过程中不需要
大量的开销,而在非随机测试技术中则需要这样的开销。
对于比本例的输入集更大的测试项“符号输入属性分解"(SIAD)树CChoe 1987)是一种有效的方
法,可以在测试用例设计前组织随机抽样的输入域。
88

## Page 95

GB/T 38634.4一2020
附 录 C
(资料性附录)
基于结构的测试设计技术的应用指南和示例
C.1 基于结构的测试的指南和示例
本附录提供了 5.3 和 6.3 中描述的基于结构的测试设计技术的指南和示例。每个示例遵照
GB/T 38634.2中定义的测试设计和实施过程。示例中使用了不同的应用程序和编程语言。如 5.1 所
述,尽管这里的每个示例都是适合于基于结构的测试环境,但实际上本部分中定义的大多数技术都可以
交互使用。
C.2 ”基于结构的测试设计技术示例
C.2.1 语句测试
C.2.1.1 介绍
语句测试的目的是根据选择的语句覆盖率导出覆盖测试项语句的测试用例集。该结构化的测试设
计技术分解了组成测试项的语句。
需要考虑下面两个主要的问题;
一一什么是语条?
一一哪些语句是可执行的?
通常情况下,语句应该是一个原子操作:一个语句应该完全执行或不执行，例如;
IF a THEN bENDIF
是多个语句,因为根据条件 ab 既可能执行也可能不执行。用于语句测试的语句定义不一定是语
言定义中所使用的语句定义。
我们预期的与机器代码相关联语句是可执行的。比如,我们预期以下所有的语句都是可执行的:
一一箱环和选择:
-一过程和函数调用;
一一带有初始化的变量声明:
扒变量存储的动态分配。
但是,其他大多数的变量声明是不可执行的。例如下面的代码:
at
if (b) {
ce
}
d;
任何在 b 为真CTRUE)情况下的测试用例都得盖了全部的语句。 注意如果不使用 b 为假
(FALSE)的情况下可以实现完全语名覆盖，
C.2.1.2 ”规格说明
考虑如下 Ada 语言的测试项,将正整数分为质数和合数 ,并且求合数的因子:
89

## Page 96

GB/T 38634.4一2020
1 READ (Num);
2 WHILE NOT End of File DO
3 Prime :一 TRUE;
4 FOR Factor ;= 2 TO Num DIV 2 DO
5     IF Num 一 (Num DIV Factor) * Factor = 0 THEN
6         WRITE (Factor. is a factor of. Num);
7       Prime ;= FALSE;
8     ENDIF;
9 ENDFOR;
10 IF Prime = TRUE THEN
11     WRITE (Num, ° is prime);
12 ENDIF;
13 READ (Num);
l4 ENDWHILE;
15 WRITE CEnd of prime number program);
C213 第1步:识别特征集(TD1)
测试依据中只定义了一个测试项,只需要定义一个特征集:
FS1 :找出质数和合数的函数
C.2.1.4 第2步:导出测试条件(TD2)
语句测试的测试条件是代码中可执行的语句。给每一行代码编号,对应测试条件的数量。 例如,语
名 ]工允许定义一个测试条件:
TCOND1; READ (Num); if J 1                                                 (FS1)
不再重复说明剩余的测试条件对应的源代码:
TCOND2; — 语句2                                                                (FSI)
TCOND3; “语句3                                                                                    (FS1)
TCOND1, 语句二                                                                            (FS1)
TCONDS: 语句5                                                                (FS1)
TCOND6: 语句6                                                                (FS1)
TCOND?: 语句7                                                                (FS1)
TCONDS8; 语句 10                                                               (FS1)
TCONDS; 语句 11                                                               (FS1)
TCONDI0O; 语句13                                                              (FS1)
TCONDI1: 语句15                                                              (FS1)
C215 第3步:导出测试覆盖项(TD3)
语句测试中的测试矣盖项和测试条件一致:
TCOVER1: iff 1                                               CTCONDI)
TCOVER2, 语句2                                                       (TCOND2)
TCOVER3: 语句3                                                       (TCOND3)
TCOVER4: 请句 4                                                       (TCOND4)
TCOVER5: 语句5                                                     (TCONDS)
90

## Page 97

GB/T 38634.4一2020
TCOVER6: 语句6                                                         (TCOND6)
TCOVER7: 语句7了                                               (TCOND7)
TCOVERS; 语句 10                                                 (TCONDS)
TCOVERDS: 语句 11                                              (TCONDS)
TCOVERLO: 语句 13                                                   (TCONDI3)
TCOVER11: 语句15                                          (TCOND11)
C.2.1.6 第4步:导出测试用例CTD4)

在语句测试中,每条语句应至少由一个测试用例牙盖。 首先通过识别控制流图中的子路径导出测
试用例,该子路径执行了没有被测试用例覆盖的一个或多个可执行的语句。确定执行子路径的输入和
预猜结果。重复此过程,直到达到测试要求的测试覆盖率。在本例中,只需要一个测试用例就可以履盖
代码中的所有语句,因为代码中的达代允许两个输入值覆盖所有语句(即达到 100努的语句覆盖),见
# CAL,

RC. 语句测试的测试用例
Ce ee
Co ee

注; 在该举例中;有一个测试用例以及三个独立的给入值和预期结果的集侣,因为代码中包含夺代。

C.2.1.7 第5步:汇集测试集(TD5)

只有一个测试用例,可以组合生成一个测试集,如下所示;

TS1;测试用例 1,
C.2.1.8 第6步:导出测试规程(TD6》

只需要一个规程,如下所示:

TPL: MR TS] 的测试用例。
C.2.1.9 语句测试覆盖率

根据 6.3.1 提供的公式和上述导出的测试覆盖项计算着盖率，

HEE so = 11 X 100% = 100%

PSL ahe «PP ae 4 ed aR HE YALE GAB] TF 100%

C.2.2 分支/判定测试
C.2.2.1 介绍
分支杆盖和判定材盖密切相关:。 对于只有一个人口点的测试项,100狗的分支攻盖相当于 100镭的
判定蓝盖,如果低于 100多的字盖这。则可能不一样。 用一个例子说明两个不同的蓝盖水平;
C.2.2.2 ”规格说明
组件确定一个单词在单词表中的位置 ,单词表基按字母顺序排序的除了输入单词和表,还应当向
91

## Page 98

GB/T 38634.4一2020
组件输入查找的表中的单词数量。如果在表中查找到单词,组件应当返回该单词的位置{从零开始),香
则返回“一1”。
代码来自(Kernighan and Richie 1998). 3 个判定进行了加粗:
int binsearch (char * word. struct key tab[], int n) (
int cond;
int low. high. mid;
low = 0;
high = n— 13
while (low <= high) {
mid = (low十high) / 2;
if (cond = stremp(word. tab{ mid ].word)) < 0)
high = mid 一 1;
else if Ccond > 0)
low = mid + 1;
else
return mid;
}
return “1;
}
C.2.2.3 第1步:识别特征集(TDT)
测试依据只定文了一个测试项,只需要定义一个特征集;
ES1:二分查找功能
C.2.2.4 第2步:导出测试条件(TD2)
C.2.2.4.1 导出测试条件的选项
构造程序的控制流图 ,识别分支7判定测试的测试条件。构造程序的控制流图的第一步是把程序划
分成基本块。基本块是指令的序列,没有分支进入基本块(除了开始) ,没有分支从基本块出来(除了结
BO). 每个基本块的语句将一起执行或者都不执行。 上面的程序有下面包个基本块，
int binsearch (char * word, struct key tab[]。 int n) (
int cond;
int low. high. mid;
Bl    low = 0;
high = n — 15
B2    while (low <= high) {
B3     mid = Clow-+high) / 2
if (Ccond = stremp( word, tabl mid],word)) < 0)
B4      high = mid 一 1;
BS else if (cond > 0)
B6      low = mid + 1;
B7    else
92

## Page 99

GB/T 38634.4—2020
return mid;
Bs}
B9    return 一1
}
构造控制流图,每个基本块代表一个节点,画一条从一个基本块到另一个基本块的控制转移边。下
面是可能控制转移:
BI-~B2 B3+Bt B5+B6 B6一B8
B2-~B3 B3-*B5 B5->B7T  B8-~B2
B2~~B9 B4-~~B8
得到的控制流图如图 C.1 所示。图中有一个人口点 BL 和两个出口点,B7 和 B9 。
图 C.1 二分查找控制流图
上面的控制流图不一定是人工绘制的,通常使用工具显示已经执行的判定/分支。
分支覆盖的测试条件与判定材盖的测试条件不同 ,操作步又见 C.2.2.4.2 和 C.2.2.4.3。
C.2.2.4.2 选项 2a:导出分支覆盖的测试条件(TD2)
对于分支攻羡,测试条件(BRANCH-TCOND)是分支,在控制流图中用边表示。本例中有 10 测试
条件,具体如下:
BRANCH-TCOND1: B1-B2                                   (FSI)
BRANCH-TCOND2; B2->B3                                   (FS1)
BRANCH-TCOND3; B2-*B9                                   (FS1)
BRANCH-TCOND4; B3-*B4                                   (FS1)
BRANCH-TCONDS; B3-*B5                                   (FS1)
BRANCH-TCOND6; Bi->B8                                   (FS1)
BRANCH-TCOND?7; B5-*B6                                   (FS1)
BRANCH-TCOND8: B5一B7                                   (FS1)
BRANCH-TCONDS; B6-*B8                                   (FS1)
BRANCH-TCONDIO; B8-*B2                                  (FS1)
93

## Page 100

GB/T 38634.4一2020
C.2.2.4.3 选项 2b:导出判定覆盖的测试条件(TD2)

对于判定覆盖,测试条件(DECISION-TCOND)是控制流图中的判定节点,判定节点有多个汇出的

箭头。在本例中,有三个测试条件:
DECISION-TCOND1:B2                                                                                                       (FS1)
DECISION-TCOND2: B3                                              (FS17
DECISION-TCOND3 : BS                                    (FSI)
C.2.2.5 第3步:导出测试覆盖项(TD3)
C.2.2.5.1 导出测试覆盖项的选项
分支著盖的测试覆盖项和判定覆盖不同 ,操作步骤见C.2.2.5.2 和 C.2.2.5.3。
C.2.2.5.2 选项 3a:导出分支覆盖的测试覆盖项CTD3)

对于分支覆益,测试覆兽项目是控制流图的分支,和测试条件一致。在本例中,分支材盖有 10 个测

试烧盖项.具体如下:
BRANCH-TCOVERI: B1-B2                            (BRANCH-TCOND1)
BRANCH-TCOVER2: B2->B3                            (BRANCH-TCOND2)
BRANCH-TCOVER3; B2->B9                            (BRANCH-TCOND3)
BRANCH-TCOVER(; B3-*B4                            (BRANCH-TCOND4)
BRANCH-TCOVERS:; B3-+B5                            (BRANCH-TCONDS)
BRANCH-TCOVER6; B4->B8                      (BRANCH-TCOND6)
BRANCH-TCOVER?7: B5->B6                            (BRANCH-TCOND?)
BRANCH-TCOVERS; B5->B7                            (BRANCH-TCOND8)
BRANCH-TCOVERS: B6-*B8                           (BRANCH-TCOND9)
BRANCH-TCOVERIO:; B8-=>B2                     (BRANCH-TCOND10)
C.2.2.5.3 选项 3b:导出判定覆盖的测试覆盖项(TD3)

对于判定覆盖,测试覆盖项是每个判定的结果(即 true. false). 在本例中,每个判定都有 true 和

false 的两个结果+因此有六个测试覆盖项,具体如下:
DECISION-TCOVER1; B2 = true                       (DECISION- TCOND1)
DECISION-TCOVER2; B2 = false                       (DECISION-TCONDI)
DECISION-TCOVER3: B3 = true                       (DECISION-TCOND2)
DECISION-TCOVER4: B3 = false                       (DECISION-TCOND2)
DECISION-TCOVERS: BS = true                        (DECISION-TCOND3)
DECISION-TCOVER6; B5 = false                       (DECISION-TCOND3)

AS PY GEE AT AT AS DS RS ES a YE
C.2.2.6 第4步;导出测试用例(TD4》

分支测试通过识别控制流子路径导出测试用例,子路径是测试过程中没有执行的一个或多个分支
《测试覆盖项) ,确定满足子路径的输人,确定每个测试的预期结果,重复上述步骤直到达到要求的测试
覆盖率。 类似的,判定测试通过识别控制流子路径导出测试用例 ,子路径到达尚未在测试过程中执行的
一个或多个判定,确定每个测试输大和预期结果。对于分支覆盖和判定覆盖.测试项中的任何一个单独

94

## Page 101

GB/T 38634.4一2020
测试,都会执行一个子路径,也会包舍许多判定和分支。
考虑测试用例执行子路移 Bl-~~B2 Bo, 该用例的天一 0,表示查找的表中没有条目。子路径执
行判定(B2 BO) ,达到了 176一16.7六的著盖率。路径执行 10 个分支中的 2 个分支,达到了 20%
盖率(和判定蓝盖不同) 。
现在考虑测试用例 ,执行了子路径:
Bl-+B2+B3+ Bi B8+ B2-> B3-+ B+ B6-> B8-> 82> B3-* B5-> B7
该子路径出现的条件是首先查找表中的前半部分,然后查找该部分的前半部分(第二个二分之一，
即四分之一) ,找到条目。注意两个测试用例提供了 100% Me GP
这些测试用例见表:C.2。
表 C.2 ”二分搜索测试用例
[mon Faeroe eer | vere [el
Alf
Bert
a,                Bl-+B2-+B3-*Bt     BRANCH-TCOVER
a          + B8-+B2-+B3     Ly 2s dy Se 6 74 8. 9. 10 DR
Dick       7            sel ee
> BS B6-*B8-*B2 | DECISION-TCOVER 1, 3, 4.
Eddy                 nae           _
BS B5-+B7        5:6
Fred                ==
Geolf
pt ar] [eee | taoenonms | |
chas                          Bl B2-+B9
table*                                 DECISION- TCOVER 2
Sy Se ANF Se PL HF 1 A PET PE
C.2.2.7 第5步:汇集测试集(TD5》
只需要两个测试用例 ,可以选择组合成一个测试集。
TS1:测试用例 1 和2。
C.2.2.8 第6步:导出测试规程(TD6)
只有两个测试用例和一个测试储,可以选择只定义一个测试规程。
TP1:按照测试集指定的硕序覆盖 TS1 中的所有测试用例。
C.2.2.9 分支测试覆盖率
根据 6.3.2 提供的公式和上述导出的测试覆盖项计算覆盖率，
MARK 50 “1X 100% =100%
PSUs ETS Sa a HA a AR a TA EE GAA 100% «
C.2.2.10 判定测试材盖率
根据 6:3.3 提供的公式和上述导出的测试覆盖项计算覆盖率，
Wh Kine, =x 100% =100%
DUH A 0 ae a GP aE PETA BT 100%
95

## Page 102

GB/T 38634.4一2020
C.2.3 分支条件测试.分支条件组合测试及修正条件判定覆盖(MCDC)测试
C.2.3.1 介绍

分支条件测试,分支条件组合测试和修正条件判定测试是密切相关的,覆盖测量也是相关联的。三
种测试设计技术的目的是根据选择的覆盖率,导出测试用例集荐蕾测试项的判定条件。 下面用一个例
子说明测试用例设计和测试覆狂测量方法。

C.2.3.2 ”规格说明

考虑下面的代码片段:

if A or (B and C) then

do_something:

else

do_something_else;

end if;

在判定条件中的布尔条件是A,B和C。A、B和C自身可能由复杂的表达式组成,涉及到关系运
算。例如,布尔条件 A 可以是一个表达式 XS 立。为了更加清晰地说明,下面的例子都把A.B和C
设置为简单的布尔条件。

C.2.3.3 第1步:识别特征集(TD1)

三种测试设计技术的例子都是基于相同的测试项(上面的代码片段),3 种测试技术可以定义一个
特征集:

FS1: 条件代码片段
C.2.3.4 第2步:导出测试条件(TD2)

三种测试设计技术中,控制流图的每个判定是一个测试条件。在本例中,有一个判定;

TCOND1;A or (B andC)                                                                        (PS1)

AS ih AE IEE SP SAR Re PE AL a ak A HF Ee aR
C.2.3.5 分支条件测试
C.2.3.5.1 第3步:导出测试覆盖项目(TD3)

分支条件测试检测多个条件判定中的独立条件,目的是每个独立条件和每个判定都同时有具有真值
(TRUE)和假值(FEALSE)。测试覆盖项是判定内的条件布尔值(真 TRUE/7假 FALSE)。在本例中,分
支条件测试要求布尔条件 A 分别取 TRUE 和 FALSE, fi RA EE B 4 HR TRUE 和 FALSE 和布尔条
tt CS} TRUE 和FALSE。下面的测试攻盖项符合要求:

TCOVERI:A = TRUE                                                   (TCONDI)
TCOVER2:A = FALSE                                             (TCONDI)
TCOVER3:B = TRUE                                                     (TCONDI)
TCOVERA:B = FALSE                                                 (TCONDI)
TCOVERS;:C = TRUE                                                 (TCOND1)
TCOVER6:C = FALSE                                               (TCOND1)
TCOVER7:A or (Band ©) = TRUE                                              CTCOND1)
TCOVER8:A or (B and ©) = FALSE                                          (TCONDI)
96

## Page 103

GB/T 38634.4一2020
C.2.3.5.2 第4步:导出测试用例(TD4)
通过识别控制流子路径导出分支条件覆盖的测试用例,子路径是测试过程中没有执行的一个或多
ASG SX CMM AN EE TD ,确定满足子路径的输入,确定每个测试的预期结果,重复上述步骤直到达到要求
的测试覆盖率。在本例中,表C.3 的测试输入集符合要求(注意:其他的测试输入集也可以实现分支条
{4 48 ah) 。
HCI 分支条件测试的测试用例
[wane TA | Te | Aaa | ee
[raise [parse [raise [raise [recovers |
注: FE C.3 ATM BUH BAS OL BIT EA BY
通常可以只用两个测试用例实现分支条件覆盖,和所有条件的实际布尔条件数量无关:
C.2.3.5.3 第5步:汇集测试集(TD5)
只需要两个测试用例 ,可以选择组合成一个测试集。
TS1:测试用例1 和 2。
C.2.3.5.4 第6步:导出测试规程(TD6)
只有两个测试用例和一个测试集.可以选择只定义一个测试规程。
下P1:按照测试集指定的顺序覆盖 TS1 中的所有测试用例。
C.2.3.5.5 分支条件测试覆盖率
根据 6.3.4 提供的公式和上述导出的测试覆盖项计算覆盖率，
WMH y xan) =X 100% =100%
Pate AE AP A AU TR WR EE a AS T-100%
C.2.3.6 “分支条件组合测试
C.2.3.6.1 第3步:导出测试覆盖项(TD3)
在分支条件组合测试中,测试覆盖项是判定中的条件布尔值的不重复组合。在本例中,分支条件组
合测试所有布尔条件 AB 和人的组台。该技术的测试覆盖项有:
TCOVERI;A = FALSE.B = FALSE.C = FALSE                       (TCONDI)
TCOVER2;A = TRUE,B = FALSE.C = FALSE                        (TCOND1)
TCOVER3:A = FALSE.B = TRUE,C = FALSE                        (TCONDI)
TCOVER(:A = TRUE,B = TRUE,C = FALSE                       (TCONDI)
TCOVERS:A = FALSE,B = FALSE,C = TRUE                       (TCOND1)
TCOVER6:A = TRUE.B = FALSE.C = TRUE                         CTCOND1)
TCOVER7:A = FALSE.B = TRUE.C = TRUE                        (TCONDI)
TCOVERS;A = TRUE.B = TRUE,C = TRUE                         ¢€TCONDI)
97

## Page 104

GB/T 38634.4一2020
C.2.3.6.2 第4步:导出测试用例(TD4)

通过识别控制流子路径导出测试用例,子路径是测试过程中没有执行的一个或多个分支(测试覆盖
项)*确定满足子路径的输入,确定每个调试的预期结果,重复上述步葬直到达到要求的测试覆盖率，在
本例中,可以通过导出表 Cok 的测试用例来实现。

表 C.4 分支条件组合测试的测试用例
aa TATOOS
pw em To |
Pe se es ov |
人 ov |
ps raise [raise ue eons
pe aor ans: ne reowers
Pe fame ee Peeve reovens
注: ECA PAE A IE IR EAR SE EY

分支条件组合覆盖是很详尽的,需要:2" 个测试用例实现 100% WS A Tn PHAR.
杂的条件不能实现这种增速，
C.2.3.6.3 第5步:汇集测试集(TD5)

所有放试用例可以组合成一个测试集,如下:

TS1: 测试用例 1,2,3,4,5.6.7,8。
C.2.3.6.4 第6步:导出测试规程(TD6)

只有一个测试集,可以选择定义一个对应的测试规程,具体如下:

TP le 按照测试集指定的顺序蓝盖 TS1 中的所有测试用例。
C.2.3.6.5 分支条件组合测试覆盖率

根据 6.3.5 提供的公式和上述生成的测试覆盖项计算覆盖率，

WR eeame =ix 100% =100%

PA REF AK AE AL Ee A i EY GA A 100%
C.2.3.7 修正条件判定覆盖测试

修正条件判定覆盖CMCDC) 测试比分支条件组全覆盖需要更少的测试用例。广泛应用于航空电子
软件的发展、 WM RTCA / DO-178C 所要求的，MCDC 测试需要测试用例说明每一个布尔条件(A.B 和
C)可以独立影响决策结果。测试覆盖项比所有条件的给合少(分支条件组合履盖所要求的)。
C.2.3.7.1 第3步:导出测试覆盖项(TD3)

在修正条件判定材盖CMCDC )涡试中,测试覆盖项是判定中的条件独立布尔值的不重复组侣,允许

98

## Page 105

GB/T 38634.4一2020
一个布尔条件单独影响结果，
例如判定条件[A or(B and C),有一个测试覆盖项对,B AC 保持不变,仅改变 A 的状态时会改变
结果,即 A 可以独立影响条件的结果,如下:
TCOVER1A=FALSE,B=FALSE,C=TRUE OUTCOME=FALSE           (《TCOND1》
TCOVER2: A= TRUE.B=FALSE.C=TRUE OUTCOME=TRUE     (TCOND1)
FEF B,需要一个测试用例对说明 A 和:C PRE AR ET BY th Si
TCOVER3:A=FALSE.B=FALSE:C=TRUE OUTCOME=FALSE              (TCOND1)
TCOVER4:A=FALSE,B=TRUE,C=TRUE OUTCOME=TRUE     (TCONDI)
MF CGB — TA A AA BRP AR AES CT Pe ae
TCOVERS: A=FALSE.B=TRUE.C=FALSE OQUTCOME=FALSE      (TCONDI1)
TCOVERG;A=FALSE.B=TRUE.C=TRUE OUTCOME=TRUE     (TCONDI)
单独为每个判定条件创建测试条件对,可以得出 TCOVERI 和 TCOVERS 是相同的,TCOVER4
和 TCOVER6 也是相同的,因此,在下一步中不会使用重复的测试覆盖项 TCOVER3 和 TCOVER6
导出测试用例。
C.2.3.7.2 第4步:导出测试用例(TD4)
通过识别控制流子路径导出测试用例 ,子路径是测试过程中设有热行的一个或多个分支(测试覆盖
项) ,确定满足子路径的输入,确定每个测试的预期结果;重复上述步骤直到达到要求的测试秆盖率。在
本例中,表C.5 的测试用例集符合要求。
BE ee C.5 中的测试用三不包括预期缚取 ,是不"完整"的，
RCS 整体测试用例集
[wen [Ta TiC]
a ee
TCOVERS
pe fawue [rats [ror foe reovere |
fre fone
TCOVERS6
[sears [eur rarse [raise covers
GH:
— BR A 2 说明 A 独立影响判定条件的结果;
-一测试用例 1 和3 说明 B 独立影响判定条件的结果;
-一测试用例 3 和二说明:C 独立影响判定条件的结果。
注意可能会有实现 MCDC 的替代方案。例如,表C.6中的两个测试用例可以说明 A 独立影响判定
条件的结果。

表 C.6 BH MCDC 测试用例
ee
a

99

## Page 106

GB/T 38634.4一2020

测试用例 X 与上面的测试用例 4 一样,得是测试用例Y 是前面没有使用的。 已经实现 MCDC, AS
需要测试用例工实现覆盖。

达到 100冶的修正条件判定覆盖率 ,至少需要?十1 个测试用例,最多 2 不测试用例,其中并是布
尔条件的数量。分支条件组合覆盖需要 2" A. Pat, MCC 是一种实用的低风险的方法,是
分支条件组人台覆贡的折中,条件琢达式涉及的不仅仅是一些布尔条件。
C.2.3.7.3 第5步:汇集测试集(TD5)

将表C:6 中定义的所有测试用例组合成一个测试集,如下，

TSl; 测试用例1,， 2，3，45
C.2.3.7.4 第6步:导出测试规程(TD6)

只有一个测试集,可以选择定义一个相应的测试规程,如下:

TP1: 按照测试集指定的顺序覆盖 TS1 中的所有测试用例。
C.2.3.7.5 “修正条件判定覆盖测试覆盖率

根据 6.3.6 提供的公式和上述导出的测试覆盖项计算覆盖率:

WHR sere =+%100%=100%

DAU aH ab AP TE he AE A ea a a a «Pa RA 100% «
C.2.3.8 其他布尔表达式

三种测试设计技术和测试覆盖测量方法的一个缺点是,当控制判定放置在实际判定条件外部时 ,布
尔表达式是有漏洞的。例如:

FLAG ;= A or (Band ©);

if FLAG then

do_something;
else
do_something_else;

end if;

JT WB RP AWE HD 5 i SUM aa AS A ie PA DS a EAT As Pe TAR AY Mk AS
是直接用于控制流判定的布尔表达式。
C.2.3.9 优化表达式

一些编程语童和编译器通过忽略表达式中对结果没有直接影响的部分“短路"布尔运算符的评估。

例如在 C和C十十语言中短路布尔*and"(&&7和*or"(|)的运算符,Ada 编程语言提和拱专门的短
路运算符: 上述例子中,如果可以根据第一个条件确定结果,则不需要评估第二个条件。

结果是,不能显示第二个条件的值覆六。对于短路“and"运算符,可行的组合是*真:真光页:候”和
“假:X”,其中X是未知的。对于短路“or"运算符,可行的组合是“假:假近假;真”和*真:X"。

其他语言和编译器可能会短路布尔运算符的评价。在该情癌下 ,可行的组合是未知的。布尔运算
符的短路优化的程度可能取决于缩译器转换器,或者可能不受用户控制。

短路控制形式对分支条件覆盖或修正条件判定覆盖没有阻碍,但会妨碍分支条件组侣覆盖的测量。
情况是,可以设计测试用例达到 100冯的覆盖率上从理论的角凑来看),但实际上测量不可能达到 100六
的覆盖。

100

## Page 107

GB/T 38634.4一2020
C.2.3.10 其他分支和判定
上述对分支条件测试.分支条件组合测试和MCDC 测试及其相应的覆盖测量用分支或判定的形式
说明,分支和判定由布尔条件控制。其他分支和判定,如多路分支(由“case” “ switch” && “computed
goto"语句实现),计算循环(由”for"或“do"实施循环不附带任何条件)不使用布尔条件,因此前面的描
述不能解决其他分支和判定。
处理该情况的一个方法是使用这三个测试设计技术及其相关的覆盖测量补充分支测试和分支或判
定覆盖。分支测试将解决所有简单的判定`多路判定和所有循环。条件测试将解决包括布尔条件的
判定。
实际上 ,其中一个选项的测试用例实现 100%%的蓝盖率,其他选项也能达到 100% AYE a, 两个
选项之间不能比较小于 100% BY A at
C.2.4 数据流测试
C.2.4.1 介绍
数据流测试的目的是通过选择的定义-使用覆盖率导出测试用例集,这些测试用例材盖测试项中变
量定义和使用之间的路从数据流测试是一项基于结构的测试设计技术,目的是执行从测试项的变量
定义点到变量引用点的子路径，子路径就是定义-使用对。不同的数据流蓝盖准则要求执行不同定义-
使用对和子路径。每个准则导出测试用例集以尽可能达到 100奴的覆盖率。
注: 数据流测试需要定义数据对象，工具通常把数组或记录当作单独的数据项,而不是作为有许多部分组成的复
全对象。 忽略复合对象的组成部分降低了数据流测试的有效竹。
C.2.4.2 ”规格说明
考虑 Ada 编程语言中对以下测试项进行数据流测试:
procedure Solve_Quadratic(A. B, C; in Float; Is_Complex: out Boolean; R1, R2;out Float) is
~ Is_Complex is true if the roots are not real.
~ If the two roots are real, they are produced in R1, R2.
Discrim ; Float:= B* B= 4.0*# A*C;                    一1
R1, R2; Float;                                                  ie
Begin                                                                      一3
if Diserim < 0.0 then                                                   =4
Is_Complex :一 true;                                        <5
else                                                                   一6
Is_Complex := false;                                        一了
end if;                                                                一8
if not Is_Complex then                                                    ~9
RL :=一(一B + Saqrt(Diserim))/ (2.0 * A);                     一10
R2 ;= (一B — Sqrt(Discrim))/ (2.0 * A);                     一
end if;                                                                ~ 12
end Solve_Quadratic;                                                       ~- 13
注: 第二行不是定义(RI AR) PAP Co a Pa ae a OD
C.2.4.3 第1步:识别特征集(TD1)
只有一个功能的测试,只要定义一个特征集:
101

## Page 108

GB/T 38634.4一2020
FS1; Solve_Quadratic 功能
C.2.4.4 第2步:导出测试条件(TD2》
第一步是列出测试项中使用的变量:A, By Cy Discrim, ls_Complexy R1 and R2。接下来,测试项
中变量每次出现都是对程序列表的灾叉引用,并为其分配一个类别(定义、谓词使用或计算使用)。变量
的出现及其类别见表:C,7。
表C.7 变量的出现及其类别
,
a Sc |
a lv
a
pe fami
a a
a
eS |
peices
测试条件是定义-使用对。
下一步,是确定定义-使用对及其类型(谓词使用或计算使用),每个定义-使用对是一个测试烤盖
项,识别定义列中的每个条目到该变量谓词使用或计算合用列的每个条目之间的链接。 定义-使用对及
其类型见表 C.8。
RCE 定义-使用对及其类型
证双全有
AT ro
ro
To |
AT | ro |
|
102

## Page 109

GB/T 38634.4一2020
表(C.8(续)
定义全对 | |
《起始行-~结束行3               计算使用                    谓词使用
en ee
0-+11
ee |
a
人 oo |
Pig reo
a
a |
Poet 0x018
es
a |
[re
BE cA TSF AM Oh A se EA A ae Ce)
C245 全定义测试
C.2.4.5.1 第3a步:导出测试覆盖项(TD3)
全定义测试中,测试覆盖项是从变量定义到使用(计算使用或谓词使用?的控制流子路径:
表C.9 所未的定义-使用对符合此准则的，
RCI 全定义测试
pope eed
freovers fe fo feo
frcovens aes ON
a
freovens fee Freon
C.2.4.5.2 第 4a步:导出测试用例CTD4)
通过识别控制流子路径,材盖一个或多个未执行的测试对盖项,确定控制子路径的输入和测试的预
103

## Page 110

GB/T 38634.4一2020
期结果,从而导出测试用例, 重复上述步强直到达到要求的测试覆盖率。 为达到 100色全定义数据流
枝善率,至少执行每个变量定义到使用(谓词使用或计算使用7的一个子路径。表C.10 的测试集合满足
此要求。
HClO 对全定义测试的测试用例
—
a [exwna | sme | wawat [ae [ Cc [income] mi |e |
fe [ase fase freowees |
TCOVERI,
A.B.C. Dis-|0-*1         Or             TCOVER?,
cnm          1-4          1+2-3+4         TCOVERS                      TRUE     未定义 | 未定义
TCOVER4
comix [ea ov |
C.2.4.5.3 全定义测试覆盖率
根据 6.3.7.1 提供的公式和上述导出的测试蓝盖项计算覆盖率:
MEM ens = 2X 100% =100%
PAM ae « aE a A a AD EAA A TF 100%
C.2.4.6 全计算使用测试
C.2.4.6.1 第3b步:导出测试覆盖项(TD3)
在全计算使用测试中,测试覆盖项是从变量定义到该定义所有计算使用的控制流子路径。
Be CALL 所示的定义-使用对符合此准则 。
RCM 全计算使用测试
测试覆盖项
|
frcovens fe fea Frenne
ficovens ee om |
ficovers rr freon |
[ww am ae freon |
104

## Page 111

GB/T 38634.4一2020
表 C.11(续)
Wit MH
js | ae re a
| [ks pee
frcovensy fae fies five COND
freovenie comps [ovis fewest EONS
ee oo oo |
C.2.4.6.2 第 4b 步:导出测试用例(TD4)
通过识别控制流子路径,复盖一个或多个未执行的测试猎盖项,确定控制子路径的输入和测试的预
期结果,从而导出测试用例 ,重复上述步骤直到达到要求的测试覆盖率: 为达到 100儿全计算使用数据
流蓝盖率,至少执行每个变量定义色该定义的所有计算使用的一个子路径,由此产生的测试用例见
表 C.12。
表 C.12 全计算使用测试的测试用例
mms
a Jexwnn| eae | wma [x] e[e[icomie] me | Re |
TCOVERL.
ABC loi                         TCOVER?2.
TCOVERS
人
ALB       0-10
7-89-10                  TCOVERS
ALB         o-ell        0-3-2-3-4-6-     TCOVERS,.
”                         7-8-9-10-11 |TCOVER7
8-9-10
Discrim
1-2-3-4-6-T-
1-11                        TCOVERS
pe Feawen [oor
fess feria freovnen |
fe fiisisaseasfreowee |
7-8-9-10-11-
[= |iscomples [ovis [eweieas freowemie [a [a [a | wee [eaex | a |
C.2.4.6.3 全计算使用测试覆盖率
根据 6.3.7.2 提供的公式和上述导出的测试枝盖项计算更盖率，
MRR cine =ix 100% = 100%
PAG REF ee A a RRA 100% 。
105.

## Page 112

GB/T 38634.4一2020
C.2.4.7 全谓词使用测试
C.2.4.7.1 第3c步;导出测试覆盖项(TD3)

在全谓词使用测试中,测试覆盖项是从变量定义到该定义所有谓词-使用的控制流子路径。

Be C.13 所示的定义使用对符合此准则。

表 C.13 全谓词使用测试
测试里普项

et eee ee
|reovert pm et tens |
preovens [Games fo
Preovers | Complex fe TCONDIZ
C.2.4.7.2 第4c步:导出测试用例(TD4)

通过识别控制流子路径,覆盖一个或多个未执行的测试覆盖项,确定控制子路径的和输大和测试的预
期结果,从而导出测试用例,重复上述步又直到达到要求的测试著盖率。为达到 100针全使用数据流覆
盖率,至少执行每个变量定义到该定义的所有使用(包括谓词使用和计算使用)的一个子路径。表C.14
的测试集满足此要求,

表 C.14 全谓词使用测试的测试用例
| mit fete | eee | mitmaee | A | BC | IsCompkex| RE | Re
Pi [econo | Foe [pes | veovwee [vf [a | raise [—1 | — |
Complex 59 sso [covers |

C.2.4.7.3 全谓词使用测试覆盖率

根据 6.3.7.3 提供的公式和上述导出的测试覆盖项计算覆盖率，

MBM onan =X 100% =100%

PSU se RY Sa ad ibe FD A aE PRGA 100% .
C.2.4.8 全使用测试
C.2.4.8.1 第3d步:导出测试覆盖项(TD3)

在全使用测试中,测试覆盖项是从变量定义到该定义的所有使用(包括谓词使用和计算使用)的控
和制流子路径。

表C.15 所示的定义-使用对符合该准则。

106

## Page 113

GB/T 38634.4一2020
RCS 全使用测试
全使用/全定义使用路筷
We aH
fom ASN
[RN
coven‘ _‘praveereoi————~ireom
coven pis _ovaveeraeien (reo
[II
[IC
coven |owein pon ——(rtaeeresinn————ireoxmn ——*|
[ee ss ies io
eovrnie comin ssi) SSSSCiON
ovens ex ses ome io
coven ews ies SiON
C.2.4.8.2 第4d步:导出测试用例(TD4)
通过识别控制流子路移导出测坛用例.子路径是测试过程中没有执行的一个或多个测试利益项.确
定满足子路径的输入,确定每个测试的预期结果,重复上述步骤直到达到要求的测试覆盖率。达到
100闪的全使用数据流著盖至少执行一个从每个变量定义到定义的每个使用(谓词使用和计算使用)的
子路径。表C.16 的调试集满足要求。
表 C.16 ”全使用测试的测试用例
本
aan  Wow
TCOVER1，
Ay B.C     0-1                        TCOVER?.
TCOVERS
1                     0-1-2-3+4-6-7- |TCOVERS,              FALSE    —t    一上
ALB        Or=10
ee ee
站
OTL
§-9-10-11             TCOVERT
107

## Page 114

GB/T 38634.4一2020
表 C.16(续)
an [exam | ene [mama [a] ole [income] we [ee |
a
toe fie fp
Diserim                  9-10
9-10-11
iets we rova |
[IITEIZI
pom ow rove |
Is_Complex                             TCOVERI6
12-13
cmos oa (peste freoveRs
C.2.4.8.3 全使用测试覆盖率
根据 6.3.7.4 提供的公式和上述导出的测试覆盖项计算吐盖率:
MR em = 18x 100%~100%
PA ate Re 0 AD RD a AE ie AE te A ST 100% 。
C.2.4.9 全定义-使用路径测试
C.2.4.9.1 第3e步:导出测试覆盖项(TD3)
为达到 100冯的全定义使用路径数据流槛盖,应执行所有变重定义到使用的“简单子路径”。 不同
于全使用测试.应执行定义使用对之间的所有简单子路径。在表C.17 表示的全定义-使用路径中,似乎
测试项中有 0-1-4-5-9-10 和 1-4-5-9-10 两个子路径未在所有测试用例中标识出来。然而,这两个子路径
是不可行(因此无法导出测试用倒来执行它们)。因此,对于全定义使用路径测试,它们不被认为是*简
单子路径”。
RCA 全定义-使用路径测试
测试医盖项
有
frcovens om |
frcovens re om |
108

## Page 115

GB/T 38634.4一2020
3 C.17 (续)
IE wm
RM Ra
ee 7 | |
coven [avoir [ovavaeveninnn —freonor |
frcovens owen rst ie SiO
frcovew wow co area ‘freon |
[av em —_[imti__|t-aave7eox0an os |
[aaa saw ke on
fcoveris exis imnniee comin
ae ee
fcovenis was ss os |
LTCovERIE [Is Complex [fri 7sst0lt3 JrTCOxe |
C.2.4.9.2 第4e步:导出测试用例(TD4)
导出全定义-使用路径的测试用例。 全使用测试导出的同一给测试用例,对于本例中的全定义-使
用路径测试.也可以达到最大的测试政盖项履盖举,如表 C.18 所示。
RCA 全定义-使用路径测试的测试用例
本
“au [axune| rae [wana [a]n [clic] wi |
TCOVER1，
A.B,G |0—*1                      TCOVER2，
pew ex ke Fee
8-9-10            TCOVERS
Se                           0-1-2-3-4-6-7- |TCOVERS,
pee toa Groomer |
ro frase — froovem
1-2-3-4-6-7-8-
Is_Complex
ii foais ata roowens
enon irs roowmn
12-13
109
