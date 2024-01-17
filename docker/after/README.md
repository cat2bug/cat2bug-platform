## Image Introduce

This image is the database container of Cat2Bug Platform, which is a service component in its system. There are four container services in this system that need to be started simultaneously to run the system normally. The image list is as follows:

*  cat2bug/cat2bug-platform-web-after
*  cat2bug/cat2bug-platform-web-front
*  cat2bug/cat2bug-platform-mysql
*  redis

## Start database container

### Quick Start

```
docker run -it -p 3306:3306 --name cat2bug-platform-web-after cat2bug/cat2bug-platform-web-after:laster
```

### Map database data to the host's disk

```
docker run -it -p 3306:3306 -v /cat2bug-platform/after/uploadPath:/home/uploadPath -v /cat2bug-platform/after/logs:/home/logs --name cat2bug-platform-web-after cat2bug/cat2bug-platform-web-after:laster
```

## Cat2Bug Platform Introduction

Cat2Bug Platform is a permanent, free, and open-source bug management platform that will be fully reserved for individuals and groups to use for free.

Its target audience is individuals or small to medium-sized software development teams. The concept of Cat2Bug is to eliminate various heavy management in project management, allowing individuals or teams to quickly get started and control software quality.

The platform is developed using JAVA+VUE and supports deployment and use on various system platforms.

### Function Introduction

1. Team management: Manage projects and members within the team.
2. Project management: Manage defects and members in the project.
3. Defect management: Manage bugs, requirements, and tasks.
4. Module management: Maintain software modules in the project.

### Experience

- Account：demo
- Password：123456

Demo Url：http://www.cat2bug.com/demo/cat2bug-platform

### Starting a container through Docker Compose

```
curl -o docker-compose.yml http://cat2bug.com/docker/docker-compose.yml
docker-compose up -d
```

### Demonstration diagram

<table>
    <tr>
        <td><img src="http://www.cat2bug.com/public/cat2bug-platform/images/1.jpg"/></td>
        <td><img src="http://www.cat2bug.com/public/cat2bug-platform/images/4.png"/></td>
    </tr>
    <tr>
        <td><img src="http://www.cat2bug.com/public/cat2bug-platform/images/3.png"/></td>
        <td><img src="http://www.cat2bug.com/public/cat2bug-platform/images/2.png"/></td>
    </tr>
    <tr>
        <td><img src="http://www.cat2bug.com/public/cat2bug-platform/images/5.png"/></td>
        <td><img src="http://www.cat2bug.com/public/cat2bug-platform/images/6.png"/></td>
    </tr>
    <tr>
        <td><img src="http://www.cat2bug.com/public/cat2bug-platform/images/7.png"/></td>
        <td><img src="http://www.cat2bug.com/public/cat2bug-platform/images/8.png"/></td>
    </tr>
</table>

## Cat2Bug communication group

QQ Group： [![Join QQ group](https://img.shields.io/badge/%E6%9C%AA%E6%BB%A1-731462000-blue?link=https%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Fk%3DG_vJa478flcFo_1ohJxNYD0mRKafQ7I1%26jump_from%3Dwebapi%26authKey%3DEL0KrLpnjYWqNN9YXTVksNlNFrV9DHYyPMx2RVOhXqLzfnmc%2BOz8oQ38aBOGx90t
)](https://qm.qq.com/cgi-bin/qm/qr?k=G_vJa478flcFo_1ohJxNYD0mRKafQ7I1&jump_from=webapi&authKey=EL0KrLpnjYWqNN9YXTVksNlNFrV9DHYyPMx2RVOhXqLzfnmc+Oz8oQ38aBOGx90t)