package com.example.library.service;

import com.example.library.entity.Book;
import com.example.library.entity.User;
import com.example.library.repository.BookRepository;
import com.example.library.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Random;

@Service
@Order(0)
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true", matchIfMissing = false)
public class DataLoadService implements CommandLineRunner {
    
    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Optional external seed file. It is deliberately empty by default so a
     * clean checkout never creates users with credentials embedded in source.
     */
    @Value("${app.demo-data.users-file:}")
    private String usersFile;
    
    private Random random = new Random();
    
    @Override
    public void run(String... args) throws Exception {
        loadUsers();
        loadBooks();
        trimBooksToLimit();
    }
    
    private void loadUsers() {
        if (usersFile == null || usersFile.isBlank()) {
            return;
        }

        Path path = Paths.get(usersFile);
        if (!Files.exists(path)) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    String userId = parts[0].trim();
                    String password = passwordEncoder.encode(parts[1].trim());
                    String name = parts[2].trim();
                    String role = parts[3].trim();
                    
                    if (!userRepository.existsById(userId)) {
                        User user = new User();
                        user.setUserId(userId);
                        user.setPassword(password);
                        user.setName(name);
                        user.setRole(role);
                        userRepository.save(user);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("无法读取演示用户种子文件: " + path, e);
        }
    }
    
    private void loadBooks() {
        Path path = Paths.get("books.txt");
        if (!Files.exists(path)) {
            createDefaultBooks();
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader("books.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 8) {
                    String isbn = parts[0].trim();
                    
                    if (!bookRepository.existsById(isbn)) {
                        Book book = new Book();
                        book.setIsbn(isbn);
                        book.setTitle(parts[1].trim());
                        book.setAuthor(parts[2].trim());
                        book.setPublisher(parts[3].trim());
                        book.setCategory(parts[4].trim());
                        book.setTotalCount(Integer.parseInt(parts[5].trim()));
                        book.setBorrowedCount(Integer.parseInt(parts[6].trim()));
                        book.setLocation(parts[7].trim());
                        
                        if (parts.length > 8) {
                            book.setKeywords(parts[8].trim());
                        }
                        if (parts.length > 9) {
                            book.setDescription(parts[9].trim());
                        }
                        
                        bookRepository.save(book);
                    }
                }
            }
        } catch (IOException e) {
            createDefaultBooks();
        }
    }
    
    private void createDefaultBooks() {
        // 如果数据库已有书籍数据，跳过加载
        if (bookRepository.count() > 0) {
            return;
        }
        
        int count = 0;
        
        // 科幻小说 (60本)
        String[][] sciFiBooks = {
            {"三体", "刘慈欣", "重庆出版社"},
            {"三体II：黑暗森林", "刘慈欣", "重庆出版社"},
            {"三体III：死神永生", "刘慈欣", "重庆出版社"},
            {"球状闪电", "刘慈欣", "重庆出版社"},
            {"超新星纪元", "刘慈欣", "重庆出版社"},
            {"流浪地球", "刘慈欣", "长江文艺出版社"},
            {"乡村教师", "刘慈欣", "长江文艺出版社"},
            {"微纪元", "刘慈欣", "长江文艺出版社"},
            {"带上她的眼睛", "刘慈欣", "长江文艺出版社"},
            {"朝闻道", "刘慈欣", "长江文艺出版社"},
            {"时间移民", "刘慈欣", "江苏凤凰文艺出版社"},
            {"2018", "刘慈欣", "江苏凤凰文艺出版社"},
            {"白垩纪往事", "刘慈欣", "江苏凤凰文艺出版社"},
            {"全频带阻塞干扰", "刘慈欣", "长江文艺出版社"},
            {"赡养上帝", "刘慈欣", "长江文艺出版社"},
            {"赡养人类", "刘慈欣", "长江文艺出版社"},
            {"中国太阳", "刘慈欣", "长江文艺出版社"},
            {"镜子", "刘慈欣", "长江文艺出版社"},
            {"欢乐颂", "刘慈欣", "长江文艺出版社"},
            {"诗云", "刘慈欣", "长江文艺出版社"},
            {"黑客帝国", "威廉·吉布森", "上海科技教育出版社"},
            {"神经漫游者", "威廉·吉布森", "上海科技教育出版社"},
            {"雪崩", "尼尔·斯蒂芬森", "四川科学技术出版社"},
            {"银河帝国", "艾萨克·阿西莫夫", "江苏凤凰文艺出版社"},
            {"基地", "艾萨克·阿西莫夫", "江苏凤凰文艺出版社"},
            {"机器人短篇全集", "艾萨克·阿西莫夫", "江苏凤凰文艺出版社"},
            {"火星救援", "安迪·威尔", "译林出版社"},
            {"安德的游戏", "奥森·斯科特·卡德", "百花文艺出版社"},
            {"死者代言人", "奥森·斯科特·卡德", "百花文艺出版社"},
            {"安德的影子", "奥森·斯科特·卡德", "百花文艺出版社"},
            {"沙丘", "弗兰克·赫伯特", "江苏凤凰文艺出版社"},
            {"沙丘救世主", "弗兰克·赫伯特", "江苏凤凰文艺出版社"},
            {"沙丘之子", "弗兰克·赫伯特", "江苏凤凰文艺出版社"},
            {"海伯利安", "丹·西蒙斯", "吉林出版集团"},
            {"海伯利安的陨落", "丹·西蒙斯", "吉林出版集团"},
            {"深渊上的火", "弗诺·文奇", "四川科学技术出版社"},
            {"天渊", "弗诺·文奇", "四川科学技术出版社"},
            {"计算中的上帝", "罗伯特·J·索耶", "四川科学技术出版社"},
            {"恐龙文明三部曲", "罗伯特·J·索耶", "四川科学技术出版社"},
            {"星际漫游指南", "道格拉斯·亚当斯", "上海译文出版社"},
            {"时间机器", "赫伯特·乔治·威尔斯", "人民文学出版社"},
            {"世界大战", "赫伯特·乔治·威尔斯", "人民文学出版社"},
            {"隐身人", "赫伯特·乔治·威尔斯", "人民文学出版社"},
            {"弗兰肯斯坦", "玛丽·雪莱", "人民文学出版社"},
            {"地心游记", "儒勒·凡尔纳", "译林出版社"},
            {"海底两万里", "儒勒·凡尔纳", "译林出版社"},
            {"八十天环游地球", "儒勒·凡尔纳", "译林出版社"},
            {"从地球到月球", "儒勒·凡尔纳", "译林出版社"},
            {"环绕月球", "儒勒·凡尔纳", "译林出版社"},
            {"火星公主", "埃德加·赖斯·巴勒斯", "四川科学技术出版社"},
            {"星际迷航", "吉恩·罗登贝瑞", "口袋出版社"},
            {"安德的游戏", "奥森·斯科特·卡德", "百花文艺出版社"},
            {"安德的影子", "奥森·斯科特·卡德", "百花文艺出版社"},
            {"死者代言人", "奥森·斯科特·卡德", "百花文艺出版社"},
            {"外星屠异", "奥森·斯科特·卡德", "百花文艺出版社"},
            {"霸主的影子", "奥森·斯科特·卡德", "百花文艺出版社"},
            {"魔戒", "J.R.R.托尔金", "译林出版社"},
            {"精灵宝钻", "J.R.R.托尔金", "译林出版社"},
            {"霍比特人", "J.R.R.托尔金", "译林出版社"},
            {"纳尼亚传奇", "C.S.刘易斯", "译林出版社"},
            {"黑暗精灵三部曲", "R.A.萨尔瓦多", "奇幻基地"}
        };
        
        for (int i = 0; i < sciFiBooks.length && count < 20; i++) {
            String isbn = generateISBN();
            createBook(isbn, sciFiBooks[i][0], sciFiBooks[i][1], sciFiBooks[i][2], "科幻小说", 
                random.nextInt(5) + 1, random.nextInt(3), 
                "A区-" + String.format("%02d", (i / 10) + 1) + "-" + String.format("%03d", i + 1),
                generateKeywords("科幻小说"),
                generateDescription(sciFiBooks[i][0], sciFiBooks[i][1], "科幻小说"));
            count++;
        }
        
        // 推理小说 (60本)
        String[][] mysteryBooks = {
            {"白夜行", "东野圭吾", "南海出版公司"},
            {"解忧杂货店", "东野圭吾", "南海出版公司"},
            {"嫌疑人X的献身", "东野圭吾", "南海出版公司"},
            {"恶意", "东野圭吾", "南海出版公司"},
            {"新参者", "东野圭吾", "南海出版公司"},
            {"麒麟之翼", "东野圭吾", "南海出版公司"},
            {"祈祷落幕时", "东野圭吾", "南海出版公司"},
            {"放学后", "东野圭吾", "南海出版公司"},
            {"幻夜", "东野圭吾", "南海出版公司"},
            {"圣女的救济", "东野圭吾", "南海出版公司"},
            {"秘密", "东野圭吾", "南海出版公司"},
            {"时生", "东野圭吾", "南海出版公司"},
            {"宿命", "东野圭吾", "南海出版公司"},
            {"分身", "东野圭吾", "南海出版公司"},
            {"变身", "东野圭吾", "南海出版公司"},
            {"信", "东野圭吾", "南海出版公司"},
            {"彷徨之刃", "东野圭吾", "南海出版公司"},
            {"单恋", "东野圭吾", "南海出版公司"},
            {"黎明之街", "东野圭吾", "南海出版公司"},
            {"风雪追击", "东野圭吾", "现代出版社"},
            {"福尔摩斯探案全集", "柯南·道尔", "人民文学出版社"},
            {"血字的研究", "柯南·道尔", "人民文学出版社"},
            {"四签名", "柯南·道尔", "人民文学出版社"},
            {"巴斯克维尔的猎犬", "柯南·道尔", "人民文学出版社"},
            {"恐怖谷", "柯南·道尔", "人民文学出版社"},
            {"罗杰疑案", "阿加莎·克里斯蒂", "新星出版社"},
            {"东方快车谋杀案", "阿加莎·克里斯蒂", "新星出版社"},
            {"尼罗河上的惨案", "阿加莎·克里斯蒂", "新星出版社"},
            {"无人生还", "阿加莎·克里斯蒂", "新星出版社"},
            {"阳光下的罪恶", "阿加莎·克里斯蒂", "新星出版社"},
            {"ABC谋杀案", "阿加莎·克里斯蒂", "新星出版社"},
            {"谋杀启事", "阿加莎·克里斯蒂", "新星出版社"},
            {"底牌", "阿加莎·克里斯蒂", "新星出版社"},
            {"帷幕", "阿加莎·克里斯蒂", "新星出版社"},
            {"斯泰尔斯庄园奇案", "阿加莎·克里斯蒂", "新星出版社"},
            {"希腊棺材之谜", "埃勒里·奎因", "新星出版社"},
            {"X的悲剧", "埃勒里·奎因", "新星出版社"},
            {"Y的悲剧", "埃勒里·奎因", "新星出版社"},
            {"Z的悲剧", "埃勒里·奎因", "新星出版社"},
            {"哲瑞·雷恩的最后一案", "埃勒里·奎因", "新星出版社"},
            {"三口棺材", "约翰·狄克森·卡尔", "新星出版社"},
            {"犹大之窗", "约翰·狄克森·卡尔", "新星出版社"},
            {"歪曲的枢纽", "约翰·狄克森·卡尔", "新星出版社"},
            {"占星术杀人魔法", "岛田庄司", "新星出版社"},
            {"斜屋犯罪", "岛田庄司", "新星出版社"},
            {"奇想·天动", "岛田庄司", "新星出版社"},
            {"北方夕鹤2/3杀人事件", "岛田庄司", "新星出版社"},
            {"异邦骑士", "岛田庄司", "新星出版社"},
            {"眩晕", "岛田庄司", "新星出版社"},
            {"钟表馆事件", "绫辻行人", "新星出版社"},
            {"十角馆事件", "绫辻行人", "新星出版社"},
            {"黑猫馆事件", "绫辻行人", "新星出版社"},
            {"暗黑馆事件", "绫辻行人", "新星出版社"},
            {"雾越邸暴雪谜案", "绫辻行人", "人民文学出版社"},
            {"告白", "凑佳苗", "南海出版公司"},
            {"赎罪", "凑佳苗", "南海出版公司"},
            {"少女", "凑佳苗", "南海出版公司"},
            {"夜行", "森村诚一", "群众出版社"},
            {"人性的证明", "森村诚一", "群众出版社"},
            {"青春的证明", "森村诚一", "群众出版社"},
            {"野性的证明", "森村诚一", "群众出版社"}
        };
        
        for (int i = 0; i < mysteryBooks.length && count < 40; i++) {
            String isbn = generateISBN();
            createBook(isbn, mysteryBooks[i][0], mysteryBooks[i][1], mysteryBooks[i][2], "推理小说", 
                random.nextInt(5) + 1, random.nextInt(3), 
                "B区-" + String.format("%02d", (i / 10) + 1) + "-" + String.format("%03d", i + 1),
                generateKeywords("推理小说"),
                generateDescription(mysteryBooks[i][0], mysteryBooks[i][1], "推理小说"));
            count++;
        }
        
        // 文学经典 (60本)
        String[][] classicBooks = {
            {"红楼梦", "曹雪芹", "人民文学出版社"},
            {"三国演义", "罗贯中", "人民文学出版社"},
            {"水浒传", "施耐庵", "人民文学出版社"},
            {"西游记", "吴承恩", "人民文学出版社"},
            {"百年孤独", "加西亚·马尔克斯", "上海文艺出版社"},
            {"活着", "余华", "南海出版公司"},
            {"平凡的世界", "路遥", "北京十月文艺出版社"},
            {"围城", "钱钟书", "人民文学出版社"},
            {"骆驼祥子", "老舍", "人民文学出版社"},
            {"四世同堂", "老舍", "人民文学出版社"},
            {"茶馆", "老舍", "人民文学出版社"},
            {"呐喊", "鲁迅", "人民文学出版社"},
            {"彷徨", "鲁迅", "人民文学出版社"},
            {"朝花夕拾", "鲁迅", "人民文学出版社"},
            {"子夜", "茅盾", "人民文学出版社"},
            {"家", "巴金", "人民文学出版社"},
            {"春", "巴金", "人民文学出版社"},
            {"秋", "巴金", "人民文学出版社"},
            {"激流三部曲", "巴金", "人民文学出版社"},
            {"雷雨", "曹禺", "人民文学出版社"},
            {"日出", "曹禺", "人民文学出版社"},
            {"原野", "曹禺", "人民文学出版社"},
            {"女神", "郭沫若", "人民文学出版社"},
            {"繁星·春水", "冰心", "人民文学出版社"},
            {"朱自清散文集", "朱自清", "人民文学出版社"},
            {"徐志摩诗集", "徐志摩", "人民文学出版社"},
            {"沈从文小说选", "沈从文", "人民文学出版社"},
            {"边城", "沈从文", "人民文学出版社"},
            {"湘行散记", "沈从文", "人民文学出版社"},
            {"呼兰河传", "萧红", "人民文学出版社"},
            {"金锁记", "张爱玲", "上海文艺出版社"},
            {"倾城之恋", "张爱玲", "上海文艺出版社"},
            {"半生缘", "张爱玲", "北京十月文艺出版社"},
            {"红玫瑰与白玫瑰", "张爱玲", "北京十月文艺出版社"},
            {"安娜·卡列尼娜", "列夫·托尔斯泰", "人民文学出版社"},
            {"战争与和平", "列夫·托尔斯泰", "人民文学出版社"},
            {"复活", "列夫·托尔斯泰", "人民文学出版社"},
            {"童年·在人间·我的大学", "高尔基", "人民文学出版社"},
            {"母亲", "高尔基", "人民文学出版社"},
            {"钢铁是怎样炼成的", "奥斯特洛夫斯基", "人民文学出版社"},
            {"飘", "玛格丽特·米切尔", "浙江文艺出版社"},
            {"呼啸山庄", "艾米莉·勃朗特", "人民文学出版社"},
            {"简·爱", "夏洛蒂·勃朗特", "人民文学出版社"},
            {"傲慢与偏见", "简·奥斯汀", "人民文学出版社"},
            {"理智与情感", "简·奥斯汀", "人民文学出版社"},
            {"呼啸山庄", "艾米莉·勃朗特", "人民文学出版社"},
            {"大卫·科波菲尔", "狄更斯", "人民文学出版社"},
            {"双城记", "狄更斯", "人民文学出版社"},
            {"雾都孤儿", "狄更斯", "人民文学出版社"},
            {"悲惨世界", "雨果", "人民文学出版社"},
            {"巴黎圣母院", "雨果", "人民文学出版社"},
            {"九三年", "雨果", "人民文学出版社"},
            {"茶花女", "小仲马", "人民文学出版社"},
            {"基督山伯爵", "大仲马", "人民文学出版社"},
            {"三个火枪手", "大仲马", "人民文学出版社"},
            {"欧也妮·葛朗台", "巴尔扎克", "人民文学出版社"},
            {"高老头", "巴尔扎克", "人民文学出版社"},
            {"包法利夫人", "福楼拜", "人民文学出版社"},
            {"追忆似水年华", "普鲁斯特", "译林出版社"},
            {"尤利西斯", "乔伊斯", "人民文学出版社"},
            {"罪与罚", "陀思妥耶夫斯基", "人民文学出版社"},
            {"卡拉马佐夫兄弟", "陀思妥耶夫斯基", "人民文学出版社"}
        };
        
        for (int i = 0; i < classicBooks.length && count < 60; i++) {
            String isbn = generateISBN();
            createBook(isbn, classicBooks[i][0], classicBooks[i][1], classicBooks[i][2], "文学经典", 
                random.nextInt(5) + 1, random.nextInt(3), 
                "C区-" + String.format("%02d", (i / 10) + 1) + "-" + String.format("%03d", i + 1),
                generateKeywords("文学经典"),
                generateDescription(classicBooks[i][0], classicBooks[i][1], "文学经典"));
            count++;
        }
        
        // 历史 (60本)
        String[][] historyBooks = {
            {"万历十五年", "黄仁宇", "中华书局"},
            {"明朝那些事儿", "当年明月", "浙江人民出版社"},
            {"史记", "司马迁", "中华书局"},
            {"资治通鉴", "司马光", "中华书局"},
            {"中国通史", "吕思勉", "中华书局"},
            {"全球通史", "斯塔夫里阿诺斯", "北京大学出版社"},
            {"世界史纲", "赫伯特·乔治·威尔斯", "人民出版社"},
            {"罗马人的故事", "盐野七生", "中信出版社"},
            {"明朝的那些事儿", "当年明月", "浙江人民出版社"},
            {"清史稿", "赵尔巽", "中华书局"},
            {"汉书", "班固", "中华书局"},
            {"后汉书", "范晔", "中华书局"},
            {"三国志", "陈寿", "中华书局"},
            {"晋书", "房玄龄", "中华书局"},
            {"宋书", "沈约", "中华书局"},
            {"南齐书", "萧子显", "中华书局"},
            {"梁书", "姚思廉", "中华书局"},
            {"陈书", "姚思廉", "中华书局"},
            {"魏书", "魏收", "中华书局"},
            {"北齐书", "李百药", "中华书局"},
            {"周书", "令狐德棻", "中华书局"},
            {"隋书", "魏征", "中华书局"},
            {"南史", "李延寿", "中华书局"},
            {"北史", "李延寿", "中华书局"},
            {"旧唐书", "刘昫", "中华书局"},
            {"新唐书", "欧阳修", "中华书局"},
            {"旧五代史", "薛居正", "中华书局"},
            {"新五代史", "欧阳修", "中华书局"},
            {"宋史", "脱脱", "中华书局"},
            {"辽史", "脱脱", "中华书局"},
            {"金史", "脱脱", "中华书局"},
            {"元史", "宋濂", "中华书局"},
            {"明史", "张廷玉", "中华书局"},
            {"清史稿", "赵尔巽", "中华书局"},
            {"毛泽东传", "罗斯·特里尔", "中国人民大学出版社"},
            {"邓小平传", "傅高义", "中信出版社"},
            {"朱元璋传", "吴晗", "人民出版社"},
            {"康熙大帝", "二月河", "长江文艺出版社"},
            {"雍正皇帝", "二月河", "长江文艺出版社"},
            {"乾隆皇帝", "二月河", "长江文艺出版社"},
            {"明朝那些事儿", "当年明月", "浙江人民出版社"},
            {"汉朝那些事儿", "飘雪楼主", "工人出版社"},
            {"唐朝那些事儿", "冬雪心境", "中国工人出版社"},
            {"宋朝那些事儿", "高天流云", "上海文艺出版社"},
            {"元朝那些事儿", "昊天牧云", "中国工人出版社"},
            {"三国那些事儿", "昊天牧云", "中国工人出版社"},
            {"两晋那些事儿", "昊天牧云", "中国工人出版社"},
            {"五代那些事儿", "余耀华", "中国工人出版社"},
            {"春秋那些事儿", "贾志刚", "中国工人出版社"},
            {"战国那些事儿", "阿龙", "中国工人出版社"},
            {"秦朝那些事儿", "昊天牧云", "中国工人出版社"},
            {"民国那些事儿", "杨念群", "中信出版社"},
            {"细说清朝", "黎东方", "上海人民出版社"},
            {"细说三国", "黎东方", "上海人民出版社"},
            {"细说秦汉", "黎东方", "上海人民出版社"},
            {"细说隋唐", "黎东方", "上海人民出版社"},
            {"细说宋朝", "黎东方", "上海人民出版社"},
            {"万历十五年", "黄仁宇", "中华书局"},
            {"中国历代政治得失", "钱穆", "三联书店"},
            {"国史大纲", "钱穆", "商务印书馆"}
        };
        
        for (int i = 0; i < historyBooks.length && count < 80; i++) {
            String isbn = generateISBN();
            createBook(isbn, historyBooks[i][0], historyBooks[i][1], historyBooks[i][2], "历史", 
                random.nextInt(5) + 1, random.nextInt(3), 
                "D区-" + String.format("%02d", (i / 10) + 1) + "-" + String.format("%03d", i + 1),
                generateKeywords("历史"),
                generateDescription(historyBooks[i][0], historyBooks[i][1], "历史"));
            count++;
        }
        
        // 编程 (60本)
        String[][] programmingBooks = {
            {"Java编程思想", "Bruce Eckel", "机械工业出版社"},
            {"Head First Java", "Kathy Sierra", "中国电力出版社"},
            {"Python编程：从入门到实践", "Eric Matthes", "人民邮电出版社"},
            {"Python核心编程", "Wesley Chun", "人民邮电出版社"},
            {"流畅的Python", "Luciano Ramalho", "人民邮电出版社"},
            {"算法导论", "Thomas H. Cormen", "机械工业出版社"},
            {"数据结构与算法分析", "Mark Allen Weiss", "机械工业出版社"},
            {"设计模式：可复用面向对象软件的基础", "Erich Gamma", "机械工业出版社"},
            {"代码大全", "Steve McConnell", "电子工业出版社"},
            {"人月神话", "Frederick P. Brooks", "清华大学出版社"},
            {"计算机程序设计艺术", "Donald E. Knuth", "机械工业出版社"},
            {"深入理解计算机系统", "Randal E. Bryant", "机械工业出版社"},
            {"操作系统概念", "Abraham Silberschatz", "机械工业出版社"},
            {"计算机网络：自顶向下方法", "James F. Kurose", "机械工业出版社"},
            {"数据库系统概念", "Abraham Silberschatz", "机械工业出版社"},
            {"编译原理", "Alfred V. Aho", "机械工业出版社"},
            {"Effective Java", "Joshua Bloch", "机械工业出版社"},
            {"Java并发编程实战", "Brian Goetz", "机械工业出版社"},
            {"深入理解Java虚拟机", "周志明", "机械工业出版社"},
            {"Spring实战", "Craig Walls", "人民邮电出版社"},
            {"Spring Boot实战", "Craig Walls", "人民邮电出版社"},
            {"MyBatis从入门到精通", "刘增辉", "电子工业出版社"},
            {"Redis设计与实现", "黄健宏", "机械工业出版社"},
            {"MongoDB权威指南", "Kristina Chodorow", "人民邮电出版社"},
            {"Docker实战", "James Turnbull", "人民邮电出版社"},
            {"Kubernetes实战", "Marko Luksa", "人民邮电出版社"},
            {"Go语言实战", "William Kennedy", "人民邮电出版社"},
            {"Go程序设计语言", "Alan A. A. Donovan", "机械工业出版社"},
            {"深入理解Go", "Mark Summerfield", "人民邮电出版社"},
            {"Node.js实战", "Alex Banks", "人民邮电出版社"},
            {"深入浅出Node.js", "朴灵", "人民邮电出版社"},
            {"JavaScript高级程序设计", "Nicholas C. Zakas", "人民邮电出版社"},
            {"JavaScript权威指南", "David Flanagan", "机械工业出版社"},
            {"Vue.js设计与实现", "霍春阳", "电子工业出版社"},
            {"React设计模式与最佳实践", "Michele Bertoli", "人民邮电出版社"},
            {"TypeScript实战", "Boris Cherny", "人民邮电出版社"},
            {"深度学习", "Ian Goodfellow", "人民邮电出版社"},
            {"机器学习", "Tom M. Mitchell", "机械工业出版社"},
            {"统计学习方法", "李航", "清华大学出版社"},
            {"人工智能：一种现代的方法", "Stuart Russell", "清华大学出版社"},
            {"神经网络与深度学习", "Michael Nielsen", "人民邮电出版社"},
            {"动手学深度学习", "李沐", "人民邮电出版社"},
            {"自然语言处理入门", "何晗", "人民邮电出版社"},
            {"计算机视觉：算法与应用", "Richard Szeliski", "清华大学出版社"},
            {"OpenCV计算机视觉编程攻略", "Robert Laganière", "人民邮电出版社"},
            {"Git权威指南", "蒋鑫", "机械工业出版社"},
            {"GitHub入门与实践", "大塚弘记", "人民邮电出版社"},
            {"Linux命令行与shell脚本编程大全", "Richard Blum", "人民邮电出版社"},
            {"鸟哥的Linux私房菜", "鸟哥", "人民邮电出版社"},
            {"深入理解Linux内核", "Daniel P. Bovet", "机械工业出版社"},
            {"Unix编程艺术", "Eric S. Raymond", "电子工业出版社"},
            {"代码整洁之道", "Robert C. Martin", "人民邮电出版社"},
            {"敏捷软件开发", "Robert C. Martin", "清华大学出版社"},
            {"重构：改善既有代码的设计", "Martin Fowler", "人民邮电出版社"},
            {"测试驱动开发", "Kent Beck", "中国电力出版社"},
            {"领域驱动设计", "Eric Evans", "电子工业出版社"},
            {"架构整洁之道", "Robert C. Martin", "电子工业出版社"},
            {"微服务设计", "Sam Newman", "人民邮电出版社"},
            {"云原生架构", "Pratik Patel", "机械工业出版社"},
            {"分布式系统概念与设计", "George Coulouris", "机械工业出版社"},
            {"一致性与共识：分布式系统设计", "Marc Shapiro", "人民邮电出版社"}
        };
        
        for (int i = 0; i < programmingBooks.length && count < 100; i++) {
            String isbn = generateISBN();
            createBook(isbn, programmingBooks[i][0], programmingBooks[i][1], programmingBooks[i][2], "编程", 
                random.nextInt(5) + 1, random.nextInt(3), 
                "E区-" + String.format("%02d", (i / 10) + 1) + "-" + String.format("%03d", i + 1),
                generateKeywords("编程"),
                generateDescription(programmingBooks[i][0], programmingBooks[i][1], "编程"));
            count++;
        }
    }
    
    private String generateISBN() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 13; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
    
    private String generateKeywords(String category) {
        Map<String, String[]> keywordsMap = Map.of(
            "科幻小说", new String[]{"科幻,太空,未来,宇宙,星际,科技,机器人,人工智能,时间旅行,外星人"},
            "推理小说", new String[]{"推理,悬疑,侦探,破案,犯罪,谋杀,谜题,线索,真相,诡计"},
            "文学经典", new String[]{"经典,文学,名著,小说,散文,诗歌,戏剧,文学奖,诺贝尔,茅盾"},
            "历史", new String[]{"历史,古代,王朝,战争,传记,文明,考古,文物,文献,史料"},
            "编程", new String[]{"编程,代码,算法,数据结构,设计模式,框架,开发,技术,计算机,软件"}
        );
        
        String[] keywords = keywordsMap.getOrDefault(category, new String[]{});
        if (keywords.length > 0) {
            int count = random.nextInt(3) + 3;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < count && i < keywords.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(keywords[i]);
            }
            return sb.toString();
        }
        return category;
    }
    
    private String generateDescription(String title, String author, String category) {
        Map<String, String> templates = Map.of(
            "科幻小说", "《%s》是%s创作的一部经典%s，讲述了一个关于%s的精彩故事，展现了作者深邃的想象力和对未来世界的独特思考。",
            "推理小说", "《%s》是%s的代表作之一，是一部扣人心弦的%s作品，情节跌宕起伏，结局出人意料，充分展现了作者高超的叙事技巧。",
            "文学经典", "《%s》是%s的经典%s作品，以细腻的笔触描绘了%s，具有深刻的思想内涵和艺术价值，是文学史上的不朽之作。",
            "历史", "《%s》是%s撰写的一部重要%s著作，深入探讨了%s，为读者呈现了一幅生动的历史画卷，具有很高的学术价值。",
            "编程", "《%s》是%s所著的一本经典%s书籍，系统地介绍了%s，是学习和掌握相关技术的必备读物，深受广大开发者喜爱。"
        );
        
        Map<String, String> contentTemplates = Map.of(
            "科幻小说", "人类与外星文明的接触",
            "推理小说", "一桩离奇谋杀案的侦破过程",
            "文学经典", "人性的复杂与社会的变迁",
            "历史", "某个历史时期的社会风貌",
            "编程", "相关编程技术和最佳实践"
        );
        
        String template = templates.getOrDefault(category, "《%s》是%s的一部%s作品。");
        String content = contentTemplates.getOrDefault(category, "相关主题");
        
        return String.format(template, title, author, category, content);
    }
    
    private void createBook(String isbn, String title, String author, String publisher, String category, 
                           int totalCount, int borrowedCount, String location, String keywords, String description) {
        if (!bookRepository.existsById(isbn)) {
            Book book = new Book();
            book.setIsbn(isbn);
            book.setTitle(title);
            book.setAuthor(author);
            book.setPublisher(publisher);
            book.setCategory(category);
            book.setTotalCount(totalCount);
            book.setBorrowedCount(borrowedCount);
            book.setLocation(location);
            book.setKeywords(keywords);
            book.setDescription(description);
            book.setBorrowable(true);
            bookRepository.save(book);
        }
    }

    private void trimBooksToLimit() {
        final int maxBooks = 100;
        List<Book> all = new ArrayList<>(bookRepository.findAll());
        if (all.size() <= maxBooks) {
            return;
        }
        all.sort(Comparator.comparing(Book::getIsbn));
        List<Book> toKeep = all.subList(0, maxBooks);
        Set<String> keepIsbns = toKeep.stream().map(Book::getIsbn).collect(java.util.stream.Collectors.toSet());
        for (Book book : all) {
            if (!keepIsbns.contains(book.getIsbn())) {
                bookRepository.deleteById(book.getIsbn());
            }
        }
    }
}
