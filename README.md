## 当前项目模块结构
后续编码任务请按此结构进行包创建
```bash
E:.
└───src.main.java.com
            └───everypicfound
                ├───common
                │   ├───cache
                │   ├───config
                │   ├───constant
                │   ├───context
                │   ├───enums
                │   ├───event
                │   ├───exception
                │   ├───executor
                │   ├───filter
                │   ├───log
                │   ├───metric
                │   ├───ratelimit
                │   ├───response
                │   └───util
                ├───imageasset
                │   ├───application
                │   │   ├───command
                │   │   ├───dto
                │   │   ├───result
                │   │   └───service
                │   ├───domain
                │   │   ├───checker
                │   │   ├───duplicate
                │   │   ├───enums
                │   │   ├───extractor
                │   │   ├───generator
                │   │   ├───repository
                │   │   ├───service
                │   │   └───validator
                │   ├───error
                │   ├───infrastructure
                │   │   ├───converter
                │   │   ├───mapper
                │   │   ├───po
                │   │   └───repository
                │   └───interfaces
                │       ├───controller
                │       ├───request
                │       └───response
                ├───modelclient
                │   ├───api
                │   ├───domain
                │   │   ├───enums
                │   │   └───validator
                │   ├───error
                │   └───infrastructure
                │       ├───config
                │       ├───health
                │       └───http
                ├───search
                │   ├───application
                │   │   ├───command
                │   │   ├───context
                │   │   ├───pipeline
                │   │   └───service
                │   ├───config
                │   ├───domain
                │   │   ├───assembler
                │   │   ├───collection
                │   │   ├───embedding
                │   │   ├───filter
                │   │   ├───overfetch
                │   │   ├───rerank
                │   │   └───validator
                │   ├───error
                │   └───interfaces
                │       ├───controller
                │       ├───request
                │       └───response
                ├───storage
                │   ├───api
                │   ├───core
                │   ├───error
                │   └───infrastructure
                │       ├───config
                │       ├───health
                │       └───local
                ├───vectorindex
                │   ├───api
                │   ├───collection
                │   ├───domain
                │   │   └───enums
                │   ├───error
                │   └───infrastructure
                │       ├───client
                │       ├───config
                │       └───health
                └───vectorization
                    ├───api
                    ├───application
                    │   ├───processor
                    │   ├───publisher
                    │   ├───scanner
                    │   └───vectorizer
                    ├───config
                    ├───domain
                    │   ├───failure
                    │   ├───fusion
                    │   ├───model
                    │   └───retry
                    ├───error
                    └───infrastructure
                        └───publisher
```

## 启动方式

### 向量化模型端启动

环境配置
```bash
cd modelservice
# 安装依赖
pip install -r requirements.txt
# 安装pytorch
conda install pytorch==2.2.1 torchvision==0.17.1 torchaudio==2.2.1 pytorch-cuda=12.1 -c pytorch -c nvidia
```

启动
```bash
uvicorn --app-dir modelservice main:app --host 0.0.0.0 --port 8001 --workers 1
```


### qdrant向量库/mysql启动
- qdrant / mysql均运行在docker容器中，通过docker-compose.yml一起启动
1. 确认 docker-compose.yml 存在

2. 启docker
```bash
cd everypicfound-backend
docker compose up -d

# 此时使用 docker ps 能看到容器
# 运行结束以后 docker compose down 关闭容器
```

- 第一次启动创建数据库(docker里面未创建数据库时才用)
```bash
cmd /c "docker exec -i everypicfound-mysql mysql -uroot -proot everypicfound < src\main\resources\db\migration\init_imageasset.sql"

#验证
docker exec -it everypicfound-mysql mysql -uroot -proot everypicfound
SHOW TABLES;
DESC image_asset;
```


- 进入数据库
```bash
docker exec -it everypicfound-mysql mysql -uroot -proot everypicfound
```

### 后端服务启动
```bash
cd everypicfound-bcakend
mvn clean compile
mvn spring-boot:run
```
### 前端套件启动
- 先确保已经下载node
```bash
cd everypicfound-frontend
mvn install
mvn run dev
```

