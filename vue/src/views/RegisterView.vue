<template>
  <div id="register">
    <!-- 新增：返回首页按钮 -->
    <div class="back-home">
      <button @click="goHome">返回首页</button>
    </div>

    <div id="contain">
      <div id="left_card">
        <h1>轻小说</h1>
        <span>ライトノベル</span>
        <div>へようこそ</div>
      </div>
      <div id="right_card">
        <el-card class="el-card">
          <h2>用户注册</h2>
          <form class="register" action="">
            <input type="text" v-model="userRegisterForm.username" placeholder="请输入账号/手机号">
            <input type="password" v-model="userRegisterForm.password" placeholder="请输入密码">
            <input type="password" v-model="userRegisterForm.confirmPwd" placeholder="再一次输入密码">
          </form>

          <div class="message">
            <span v-html="error"></span>
          </div>
          <div id="btn">
            <button class="registerbtn" @click="userRegister">注册</button>
          </div>
          <div class="login-link">
            已有账号？<router-link to="/login">立即登录</router-link>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { reactive, ref } from 'vue'
import { register } from '@/api/auth'

const userRegisterForm = reactive({
  username: "",
  password: "",
  confirmPwd: ""
})

const error = ref('')
const router = useRouter()

const goHome = () => {
  router.push('/')
}

const userRegister = async () => {
    error.value = ''

    const uname = (userRegisterForm.username || '').trim()
    if (uname.length < 3 || uname.length > 20) {
      error.value = "<font color='red'>账号长度为 3-20 个字符！</font>"
      return
    }

    if (!userRegisterForm.password) {
      error.value = "<font color='red'>请输入密码！</font>"
      return
    }
    if (userRegisterForm.password.length < 6 || userRegisterForm.password.length > 20) {
      error.value = "<font color='red'>密码长度为 6-20 个字符！</font>"
      return
    }

    if (userRegisterForm.password !== userRegisterForm.confirmPwd) {
      error.value = "<font color='red'>两次输入的密码不一致！</font>"
      return
    }

    try {
      const res = await register(uname, userRegisterForm.password, '')
      if (res.code === 200) {
        const { token, user } = res.data
        localStorage.setItem('token', token)
        localStorage.setItem('userInfo', JSON.stringify(user))
        if (user && user.id != null) localStorage.setItem('userId', String(user.id))
        error.value = "<font color='green'>注册成功！即将跳转到首页...</font>"
        setTimeout(() => router.push('/'), 2000)
      } else {
        error.value = `<font color='red'>${res.msg || '注册失败！'}</font>`
      }
    } catch (err) {
      error.value = "<font color='red'>注册失败，请检查网络连接！</font>"
    }
  }
</script>

<style lang="less" scoped>
// 新增：返回首页按钮样式（和登录页完全一致，保证视觉统一）
.back-home {
  position: absolute;
  top: 20px;
  left: 20px;
  z-index: 2;
}

.back-home button {
  padding: 8px 16px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  background-color: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(5px);
  border-radius: 8px;
  color: white;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.3s;
}

.back-home button:hover {
  background-color: rgba(255, 255, 255, 0.2);
}

// 原有样式
:global(html, body) {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
}
:global(*) {
  box-sizing: border-box;
}

// 背景色旋转动画（和登录页一致）
@keyframes animate {
  0% {
    filter: hue-rotate(0deg);
  }
  100% {
    filter: hue-rotate(360deg);
  }
}

// 注册页外层容器（和登录页样式一致）
#register {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-image: url(../assets/注册界面图片.png);
  background-size: cover;
  background-position: center center;
  background-repeat: no-repeat;
  background-color: #a7a8bd;
  z-index: 1;
}

// 注册卡片容器（和登录页一致）
#contain {
  width: 900px;
  height: 400px;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  border-radius: 25px;
  border: 1px solid black;
  background-color: rgba(255, 255, 255, 0.1) !important;
  backdrop-filter: blur(5px);
  box-shadow: -5px -5px 10px rgb(39, 65, 65), 5px 5px 20px aqua;
  animation: animate 5s linear infinite;
  display: flex;
  flex-direction: row;
  text-align: center;
  align-items: center;
}

// 左侧文案区域（和登录页风格一致）
#left_card {
  width: 500px;
  padding: 0 20px;

  h1 {
    color: white;
    white-space: nowrap;
    text-shadow: 0 0 5px #000;
    font-size: 3rem;
  }

  span {
    font-size: 2rem;
    color: white;
    white-space: nowrap;
    text-shadow: 0 0 3px #000;
  }

  div {
    font-size: 3rem;
    color: white;
    white-space: nowrap;
    text-shadow: 0 0 3px #000;
  }
}

// 右侧注册表单区域
#right_card {
  width: 400px;
  display: flex;
  justify-content: center;
  align-items: center;

  .el-card {
    margin: 0 45px;
    border-radius: 25px;
    background-color: rgba(255, 255, 255, 0.1);
    padding: 20px;
    color: #fff;
  }

  h2 {
    margin-bottom: 15px;
    font-size: 24px;
    text-align: center;
  }

  // 注册表单
  .register {
    width: 100%;
    /* 关键：确保父容器的宽度计算包含内边距 */
    box-sizing: border-box;
    padding: 0 10px; /* 给父容器左右留10px内边距 */
  }

  .register input {
    /* 核心调整：宽度85% + 最大宽度300px，防止超出 */
    width: 85%;
    max-width: 300px;
    height: 35px;
    /* 水平居中，避免左右贴边 */
    margin: 8px auto;
    display: block;
    padding-left: 10px;
    border: 1px solid white;
    background-color: rgba(255, 255, 255, 0.5);
    border-radius: 12px;
    font-size: 18px;
    outline: none;
    color: #333;
  }
  // 提示信息
  .message {
    margin-top: 16px;
    font-size: 0.9rem;
    text-align: center;
    min-height: 20px;
  }

  // 注册按钮容器
  #btn {
    width: 100%;
    margin-top: 10px;
  }

  // 注册按钮
  .registerbtn {
    width: 100%;
    height: 40px;
    border-radius: 10px;
    background-color: rgba(207, 38, 38, 0.8);
    cursor: pointer;
    border: none;
    color: white;
    font-size: 16px;
  }

  .registerbtn:hover {
    background-color: rgba(187, 28, 28, 0.8);
  }

  // 登录链接
  .login-link {
    margin-top: 15px;
    text-align: center;
    font-size: 14px;
    color: #fff;

    a {
      color: #409eff;
      text-decoration: none;
      margin-left: 5px;
    }

    a:hover {
      text-decoration: underline;
    }
  }
}


</style>
