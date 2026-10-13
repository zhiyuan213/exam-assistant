<template><view class="card empty"><image src="/static/dengkemiao.png"/><view class="title">先给努力一个方向</view><view class="muted">创建你的第一份备考计划，从今天开始。</view><button class="primary" @click="create">创建备考计划</button></view></template>
<script setup>function create(){uni.navigateTo({url:'/pages/exam/create'})}</script>
