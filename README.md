# WebCompressor - 网页资源压缩系统

> 数据结构课程大作业 | JavaFX 17 + Maven

---

## 目录

1. [项目简介](#项目简介)
2. [环境配置](#环境配置)
3. [项目结构](#项目结构)
4. [功能特性](#功能特性)
5. [系统架构](#系统架构)
6. [核心类设计](#核心类设计)
7. [算法原理](#算法原理)
8. [使用说明](#使用说明)

---

## 项目简介

WebCompressor 是一款基于 JavaFX 开发的网页资源压缩系统，支持多种压缩算法，能够对网页文件（HTML、CSS、JS）、图片、字体等资源进行智能压缩，并生成统一的 `.wzip` 归档文件。

### 主要特性

- **多算法支持**：Huffman、LZ77、Brotli、PoolingImage、LZW
- **智能调度**：根据文件类型自动选择最优压缩算法
- **归档功能**：支持将整个网页文件夹压缩为单一 `.wzip` 文件
- **可视化分析**：实时显示压缩率、霍夫曼树结构、传输时间分析
- **跨平台**：基于 Java 17，支持 Windows、macOS、Linux

---

## 环境配置

### 第一步：安装 JDK 17

1. 访问 [Oracle JDK 下载页面](https://www.oracle.com/java/technologies/downloads/#java17) 或 [Adoptium (Eclipse Temurin)](https://adoptium.net/temurin/releases/?version=17)
2. 下载 **Windows x64 Installer** 版本
3. 运行安装程序，完成安装
4. 验证安装：
   ```
   打开命令提示符，输入：
   java -version
   ```
   应显示：`java version "17.0.x"`

### 第二步：安装 JavaFX SDK

1. 访问 [OpenJFX 下载页面](https://gluonhq.com/products/javafx/)
2. 下载 **JavaFX SDK 17** for Windows
   - 选择 `Windows` → `SDK` → 版本 `17.0.2`
3. 解压到本地目录，例如：`D:\javafx-sdk-17`

### 第三步：安装 Maven

1. 访问 [Apache Maven 下载页面](https://maven.apache.org/download.cgi)
2. 下载 `apache-maven-3.9.x-bin.zip`
3. 解压到目录，例如：`D:\apache-maven`
4. 配置环境变量：
   - 右键 `此电脑` → `属性` → `高级系统设置` → `环境变量`
   - 在 `系统变量` 中新建：
     - 变量名：`MAVEN_HOME`
     - 变量值：`D:\apache-maven`
   - 编辑 `Path`，添加：`%MAVEN_HOME%\bin`
5. 验证安装：
   ```
   mvn -version
   ```

### 第四步：运行项目

```bash
# 进入项目目录
cd WebCompressor

# 编译并运行
mvn clean compile javafx:run -Djava.module.path=D:\javafx-sdk-17\lib
```

> **注意**：请将 `D:\javafx-sdk-17` 替换为你实际解压的 JavaFX SDK 路径

---

### 常见问题

**Q: 运行时报错 "JavaFX runtime components are missing"？**

A: 确保已正确下载 JavaFX SDK 并配置了 `--module-path` 参数。

**Q: 如何打包为可执行 JAR？**

```bash
mvn clean package
java -jar --module-path D:\javafx-sdk-17\lib --add-modules javafx.controls,javafx.fxml target/WebCompressor-1.0.0.jar
```

---

## 项目结构

```
WebCompressor/
├── src/
│   ├── main/
│   │   ├── java/compressor/
│   │   │   ├── gui/              # 图形界面层
│   │   │   │   ├── App.java              # 程序入口
│   │   │   │   ├── MainController.java    # 主控制器
│   │   │   │   ├── CompressorFactory.java # 工厂模式
│   │   │   │   ├── CompressionService.java# 压缩服务
│   │   │   │   ├── ChartBuilder.java      # 图表构建
│   │   │   │   └── TreeVisualizer.java    # 树可视化
│   │   │   │
│   │   │   ├── core/             # 核心接口层
│   │   │   │   ├── ICompressor.java        # 压缩器接口
│   │   │   │   └── AbstractCompressor.java # 抽象基类
│   │   │   │
│   │   │   ├── algorithms/       # 算法实现层
│   │   │   │   ├── HuffmanCompressor.java  # 霍夫曼编码
│   │   │   │   ├── LZ77Compressor.java     # LZ77压缩
│   │   │   │   ├── BrotliCompressor.java   # Brotli压缩
│   │   │   │   ├── PoolingImageCompressor.java # 图像池化
│   │   │   │   └── LZWImageCompressor.java # LZW图像
│   │   │   │
│   │   │   ├── engine/           # 引擎层
│   │   │   │   ├── ResourceDispatcher.java # 智能调度器
│   │   │   │   └── WZipArchiver.java       # 归档器
│   │   │   │
│   │   │   ├── model/            # 数据模型层
│   │   │   │   ├── CompressionStats.java   # 压缩统计
│   │   │   │   ├── CompressionManifest.java# 压缩清单
│   │   │   │   └── FileFingerprint.java    # 文件指纹
│   │   │   │
│   │   │   └── utils/            # 工具类层
│   │   │       ├── BitInputStream.java      # 位输入流
│   │   │       ├── BitOutputStream.java     # 位输出流
│   │   │       ├── HuffmanCodeTree.java    # 霍夫曼树
│   │   │       └── FileUtils.java          # 文件工具
│   │   │
│   │   └── resources/
│   │       └── fxml/
│   │           └── MainLayout.fxml # 界面布局文件
│   │
│   └── test/                     # 测试代码
│       └── java/compressor/
│           └── algorithms/
│               ├── HuffmanCompressorTest.java
│               └── BrotliCompressorTest.java
│
├── pom.xml                       # Maven配置文件
└── README.md                     # 项目说明文档
```

---

## 功能特性

### 支持的文件类型与压缩算法

| 文件类型 | 扩展名 | 推荐算法 | 特点 |
|----------|--------|----------|------|
| 网页文本 | `.html` `.htm` `.css` `.js` | Brotli | 内置优化字典 |
| 图片文件 | `.jpg` `.png` `.gif` `.webp` | PoolingImage | 有损压缩，可调质量 |
| 字体文件 | `.woff` `.woff2` `.ttf` | LZ77 | 无损压缩 |
| 配置文件 | `.json` `.xml` `.txt` | Brotli | 高压缩率 |
| 其他类型 | - | Brotli | 通用高效 |

### 核心功能

1. **单文件压缩**：选择任意文件，使用指定算法压缩
2. **批量压缩**：选择文件夹，自动处理所有资源文件
3. **智能压缩**：启用智能模式，系统自动选择最优算法
4. **网页归档**：将整个网页文件夹打包为单个 `.wzip` 文件
5. **解压还原**：支持解压 `.wzip` 归档和单文件压缩包
6. **可视化分析**：
   - 压缩前后大小对比柱状图
   - 霍夫曼编码树结构可视化
   - 不同网络环境下的传输时间分析

---

## 系统架构

### 分层架构图

```mermaid
graph TB
    subgraph GUI["表现层 (GUI)"]
        A["App.java<br/>主程序入口"]
        B["MainController<br/>控制器"]
        C["CompressorFactory<br/>工厂模式"]
        D["ChartBuilder<br/>图表构建"]
        E["TreeVisualizer<br/>树可视化"]
    end

    subgraph CORE["核心层 (Core)"]
        F["ICompressor<br/>压缩器接口"]
        G["AbstractCompressor<br/>抽象基类"]
    end

    subgraph ALGORITHMS["算法层 (Algorithms)"]
        H["HuffmanCompressor<br/>霍夫曼编码"]
        I["LZ77Compressor<br/>滑动窗口"]
        J["BrotliCompressor<br/>Brotli压缩"]
        K["PoolingImageCompressor<br/>图像池化"]
        L["LZWImageCompressor<br/>LZW图像"]
    end

    subgraph ENGINE["引擎层 (Engine)"]
        M["ResourceDispatcher<br/>智能调度器"]
        N["WZipArchiver<br/>归档器"]
    end

    subgraph MODEL["模型层 (Model)"]
        O["CompressionStats<br/>压缩统计"]
        P["CompressionManifest<br/>压缩清单"]
        Q["FileFingerprint<br/>文件指纹"]
    end

    subgraph UTILS["工具层 (Utils)"]
        R["BitInputStream<br/>位输入流"]
        S["BitOutputStream<br/>位输出流"]
        T["HuffmanCodeTree<br/>霍夫曼树"]
        U["FileUtils<br/>文件工具"]
    end

    A --> B
    B --> C
    B --> D
    B --> E

    C --> F
    M --> F
    N --> F

    F --- G
    G --- H
    G --- I
    G --- J
    G --- K
    G --- L

    H --> T
    H --> R
    H --> S
    I --> R
    I --> S

    B --> M
    B --> N

    M -.-> H
    M -.-> I
    M -.-> J
    M -.-> K
    M -.-> L

    B --> O
    B --> P
    B --> Q

    N --> U

    class GUI layer-gui
    class CORE layer-core
    class ALGORITHMS layer-algo
    class ENGINE layer-engine
    class MODEL layer-model
    class UTILS layer-utils

    style GUI fill:#e1f5fe,stroke:#01579b,stroke-width:2px
    style CORE fill:#fff3e0,stroke:#e65100,stroke-width:2px
    style ALGORITHMS fill:#e8f5e9,stroke:#2e7d32,stroke-width:2px
    style ENGINE fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px
    style MODEL fill:#fce4ec,stroke:#c2185b,stroke-width:2px
    style UTILS fill:#fafafa,stroke:#455a64,stroke-width:2px
```

---

## 核心类设计

### UML 类图

```mermaid
classDiagram
    class ICompressor {
        <<interface>>
        +compress(byte[] data) byte[]
        +decompress(byte[] data) byte[]
        +getStats() CompressionStats
        +getAlgorithmName() String
        +getDescription() String
    }

    class AbstractCompressor {
        <<abstract>>
        #stats: CompressionStats
        +startTiming()
        +endTiming(long, long)
        +ensureCapacity(byte[], int) byte[]
        +getStats() CompressionStats
    }

    class HuffmanCompressor {
        -codeTable: Map
        -reverseCodeTable: Map
        +compress(byte[]) byte[]
        +decompress(byte[]) byte[]
        +getCodeTable() Map
        +getTreeStructure() String
    }

    class LZ77Compressor {
        -WINDOW_SIZE: int = 4096
        -LOOKAHEAD_SIZE: int = 256
        +compress(byte[]) byte[]
        +decompress(byte[]) byte[]
        -encode(byte[]) List
        -findLongestMatch(byte[], int) Match
    }

    class BrotliCompressor {
        -compressionLevel: int
        +compress(byte[]) byte[]
        +decompress(byte[]) byte[]
        +compress(String) byte[]
        +decompressToString(byte[]) String
    }

    class PoolingImageCompressor {
        -quality: int
        -defaultQuality: int
        +compress(byte[]) byte[]
        +decompress(byte[]) byte
    }

    class LZWImageCompressor {
        +compress(byte[]) byte[]
        +decompress(byte[]) byte[]
    }

    class CompressorFactory {
        <<enumeration>>
        HUFFMAN
        LZ77
        BROTLI
        LZW_IMAGE
        POOLING_IMAGE
        +createCompressor(CompressorType) ICompressor
        +getTypeFromExtension(String) CompressorType
    }

    class ResourceDispatcher {
        -instance: ResourceDispatcher
        +getInstance() ResourceDispatcher
        +getStrategyByExtension(String) byte
        +getCompressor(byte) ICompressor
        +compress(String, byte[]) CompressionResult
    }

    ICompressor <|.. AbstractCompressor
    AbstractCompressor <|-- HuffmanCompressor
    AbstractCompressor <|-- LZ77Compressor
    AbstractCompressor <|-- BrotliCompressor
    AbstractCompressor <|-- PoolingImageCompressor
    AbstractCompressor <|-- LZWImageCompressor
    CompressorFactory ..> HuffmanCompressor
    CompressorFactory ..> LZ77Compressor
    CompressorFactory ..> BrotliCompressor
    CompressorFactory ..> PoolingImageCompressor
    CompressorFactory ..> LZWImageCompressor
    ResourceDispatcher ..> ICompressor
```

### 设计模式应用

1. **策略模式 (Strategy Pattern)**
   - 定义 `ICompressor` 接口
   - 多种压缩算法实现同一接口，可相互替换

2. **工厂模式 (Factory Pattern)**
   - `CompressorFactory` 根据类型创建对应的压缩器实例
   - 隔离具体算法的创建逻辑

3. **单例模式 (Singleton Pattern)**
   - `ResourceDispatcher` 使用单例模式，确保全局唯一调度器

---

## 算法原理

### 压缩算法性能对比

| 算法 | 类型 | 适用场景 | 压缩率 | 特点 |
|------|------|----------|--------|------|
| Huffman | 无损 | 文本文件 | 50-70% | 基于字符频率的变长编码 |
| LZ77 | 无损 | 通用数据 | 40-70% | 滑动窗口+回溯引用 |
| Brotli | 无损 | 网页资源 | 60-80% | 内置优化字典，跨平台 |
| PoolingImage | 有损 | 图片文件 | 50-90% | 均值池化+有损压缩 |
| LZW | 无损 | 图片文件 | 30-60% | 字典压缩 |

### 数据处理流程图

```mermaid
flowchart TD
    START([开始]) --> SELECT[选择文件/文件夹]

    SELECT --> SM{是否启用<br/>智能模式?}

    SM -->|是| DISPATCH[ResourceDispatcher<br/>自动选择算法]
    SM -->|否| MANUAL[手动选择压缩算法]

    DISPATCH --> EXT{文件类型判断}
    MANUAL --> EXT

    EXT -->|HTML| BRO[选择 Brotli]
    EXT -->|图片| POOL[选择 PoolingImage]
    EXT -->|字体| LZ[选择 LZ77]
    EXT -->|其他| BRO2[选择 Brotli]

    BRO --> READ[读取文件数据]
    POOL --> READ
    LZ --> READ
    BRO2 --> READ

    READ --> COMPRESS{压缩算法}

    COMPRESS --> HF1[统计字符频率]
    HF1 --> HF2[构建霍夫曼树]
    HF2 --> HF3[生成编码表]
    HF3 --> HF4[变长编码输出]

    COMPRESS --> LZ1[滑动窗口扫描]
    LZ1 --> LZ2[查找最长匹配]
    LZ2 --> LZ3[输出引用]

    COMPRESS --> BR1[使用内置字典]
    BR1 --> BR2[DEFLATE压缩]
    BR2 --> BR3[输出压缩数据]

    COMPRESS --> PI1[图像解码]
    PI1 --> PI2[均值池化降采样]
    PI2 --> PI3[有损压缩输出]

    HF4 --> ARCHIVE[生成WZip归档]
    LZ3 --> ARCHIVE
    BR3 --> ARCHIVE
    PI3 --> ARCHIVE

    ARCHIVE --> SAVE[保存.wzip文件]
    SAVE --> OUTPUT[显示压缩统计]
    OUTPUT --> END([结束])

    style START fill:#4CAF50,color:#fff
    style END fill:#f44336,color:#fff
    style DISPATCH fill:#2196F3,color:#fff
    style ARCHIVE fill:#FF9800,color:#fff
```

### 智能调度决策流程

```mermaid
flowchart TD
    START([输入文件]) --> GET_EXT[获取文件扩展名]

    GET_EXT --> MATCH{匹配规则}

    MATCH -->|网页| TEXT[文本文件]
    MATCH -->|图片| IMAGE[图片文件]
    MATCH -->|字体| FONT[字体/二进制]
    MATCH -->|其他| DEFAULT[默认类型]

    TEXT --> ALGO1[算法: BrotliCompressor]
    IMAGE --> ALGO2[算法: PoolingImageCompressor]
    FONT --> ALGO3[算法: LZ77Compressor]
    DEFAULT --> ALGO4[算法: BrotliCompressor]

    ALGO1 --> RESULT1[压缩率: 60-80%]
    ALGO2 --> RESULT2[压缩率: 50-90%]
    ALGO3 --> RESULT3[压缩率: 40-70%]
    ALGO4 --> RESULT4[压缩率: 60-80%]

    RESULT1 --> FINAL[输出压缩结果]
    RESULT2 --> FINAL
    RESULT3 --> FINAL
    RESULT4 --> FINAL

    FINAL --> END([完成])

    style START fill:#4CAF50,color:#fff
    style END fill:#4CAF50,color:#fff
    style TEXT fill:#E3F2FD,stroke:#1976D2
    style IMAGE fill:#E8F5E9,stroke:#388E3C
    style FONT fill:#FFF3E0,stroke:#F57C00
    style ALGO1 fill:#BBDEFB,stroke:#1976D2
    style ALGO2 fill:#C8E6C9,stroke:#388E3C
    style ALGO3 fill:#FFE0B2,stroke:#F57C00
```

### Huffman 编码原理

```mermaid
flowchart LR
    subgraph INPUT["1. 字符频率统计"]
        A["原始文本:<br/>'ABRACADABRA'"] --> B["频率统计:<br/>A:5 B:2 R:2<br/>C:1 D:1"]
    end

    subgraph TREE["2. 构建霍夫曼树"]
        B --> C["构建过程"]
        C --> D["(A:5) (B:2) (R:2) (C:1) (D:1)"]
        D --> E["合并最小: C+D → (CD:2)"]
        E --> F["(A:5) (B:2) (R:2) (CD:2)"]
        F --> G["合并最小: B+(CD) → (BCD:4)"]
        G --> H["(A:5) (R:2) (BCD:4)"]
        H --> I["合并最小: R+(BCD) → (RBCD:6)"]
        I --> J["(A:5) (RBCD:6)"]
        J --> K["合并: A+(RBCD) → Root:11"]
    end

    subgraph CODE["3. 生成编码"]
        K --> L["左分支=0, 右分支=1"]
        K --> M["A = 1<br/>R = 01<br/>B = 000<br/>C = 0010<br/>D = 0011"]
    end

    subgraph OUTPUT["4. 编码输出"]
        M --> N["ABRACADABRA"]
        N --> O["1 000 01 1 0010 1 0011 1 000 01 1"]
        N --> P["原始: 88 bits<br/>编码: 25 bits<br/>压缩率: 71.6%"]
    end

    style INPUT fill:#e3f2fd
    style TREE fill:#fff3e0
    style CODE fill:#e8f5e9
    style OUTPUT fill:#fce4ec
```

### LZ77 滑动窗口原理

```mermaid
flowchart LR
    subgraph SLIDING["LZ77 滑动窗口机制"]
        A["缓冲区结构"]
    end

    A --> B["lookahead buffer<br/>前向缓冲区"]
    A --> C["search buffer<br/>查找缓冲区(已处理)"]

    B --- D["... A B R A C | A D A B R A ..."]
    D --- E["| 当前位置 |"]

    C --- F["查找缓冲区"]
    F --- G["... A B R A C |"]

    style F fill:#fff9c4,stroke:#f9a825
    style B fill:#c8e6c9,stroke:#388e3c

    subgraph MATCH["匹配过程"]
        H[当前: A D A B R A]
        H --> I[在查找缓冲区中搜索]
        I --> J{找到匹配?}
        J -->|是| K[记录: 偏移,长度]
        J -->|否| L[输出字面量: A]
    end

    subgraph EXAMPLE["实际示例"]
        M[原始: ABABABA]
        M --> N[处理过程:]
        N --> O[1. A 输出字面量A]
        N --> P[2. B 输出字面量B]
        N --> Q[3. ABA 输出引用2,3]
        N --> R[输出: A B 2,3]
    end

    style MATCH fill:#e8f5e9
    style EXAMPLE fill:#fff3e0
```

### Brotli 压缩原理

```mermaid
flowchart TD
    subgraph ENCODE["Brotli 压缩流程"]
        A1[原始数据] --> A2[字节流]
        A2 --> A3[上下文建模]
        A3 --> A4[查询内置字典]
        A4 --> A5[熵编码]
        A5 --> A6[压缩输出]
    end

    subgraph DICT["内置字典"]
        B1[HTML标签<br/>CSS属性<br/>JS关键字<br/>域名等]
    end

    subgraph DECODE["Brotli 解压流程"]
        C1[压缩数据] --> C2[熵解码]
        C2 --> C3[LZ77反向引用]
        C3 --> C4[上下文解码]
        C4 --> C5[原始数据]
    end

    A4 -.-> DICT
    A5 -.-> C2
```

---

## 使用说明

### 界面介绍

主界面分为以下区域：

1. **顶部工具栏**：文件选择、算法选择、压缩控制
2. **左侧文件列表**：显示待压缩文件信息
3. **中间控制台**：输出压缩过程日志
4. **右侧可视化面板**：
   - **压缩对比**：柱状图对比原始和压缩后大小
   - **树结构**：显示霍夫曼编码树
   - **传输分析**：模拟不同网络环境下的传输时间

### 基本操作

1. **压缩文件**
   - 点击"选择文件"或"选择文件夹"
   - 选择是否启用"智能匹配"
   - 点击"开始压缩"
   - 查看压缩结果和统计信息

2. **解压文件**
   - 点击"解压"按钮
   - 选择压缩文件
   - 选择输出目录
   - 完成解压

3. **网页归档**
   - 点击"网页归档"
   - 选择网页文件夹
   - 保存为 `.wzip` 文件

### 图表导出

本 README 中的图表使用 Mermaid 语法编写，可通过以下方式导出为图片：

1. **在线编辑**：访问 [Mermaid Live Editor](https://mermaid.live)，粘贴代码并导出
2. **VS Code 插件**：安装 Mermaid 插件，预览后右键导出
3. **draw.io**：打开 https://app.diagrams.net，选择 "插入" → "高级" → "Mermaid"

---

## 技术栈

- **语言**：Java 17
- **GUI**：JavaFX 17
- **构建工具**：Maven 3.9+
- **IDE**：IntelliJ IDEA / VS Code

---

## 许可证

本项目仅用于课程学习交流。

---

> 最后更新：2026年4月
