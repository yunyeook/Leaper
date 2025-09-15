let websocket = null;
let isConnected = false;
let isJoined = false;
let currentChatRoomId = null;
let currentPartnerId = null;
let oldestMessageId = null; // 페이징용
let isLoadingMessages = false; // 중복 로딩 방지
let hasMoreMessages = true; // 더 불러올 메시지 여부
let jwtToken = null; // JWT 토큰 저장

// DOM 요소들
const elements = {
    connectBtn: document.getElementById('connectBtn'),
    leaveBtn: document.getElementById('leaveBtn'),
    sendBtn: document.getElementById('sendBtn'),
    sendFileBtn: document.getElementById('sendFileBtn'),
    connectionStatus: document.getElementById('connectionStatus'),
    messageInput: document.getElementById('messageInput'),
    fileInput: document.getElementById('fileInput'),
    messages: document.getElementById('messages'),
    logs: document.getElementById('logs'),
    connectStatus: document.getElementById('connectStatus'),
    connectStatusText: document.getElementById('connectStatusText'),
    chatContainer: document.getElementById('chatContainer'),
    chatRoomTitle: document.getElementById('chatRoomTitle'),
    textMessageDiv: document.getElementById('textMessageDiv'),
    fileMessageDiv: document.getElementById('fileMessageDiv'),
    authenticateBtn: document.getElementById('authenticateBtn'),
    authStatus: document.getElementById('authStatus'),
    authStatusText: document.getElementById('authStatusText')
};

// WebSocket 연결 (자동 연결)
function connect() {
    const wsUrl = 'ws://localhost:8080/ws/chat';

    try {
        websocket = new WebSocket(wsUrl);

        websocket.onopen = function(event) {
            log('WebSocket 연결 성공', 'system');
            isConnected = true;
            updateConnectionStatus();
        };

        websocket.onmessage = function(event) {
            const message = JSON.parse(event.data);
            log(`수신: ${event.data}`, 'received');
            handleIncomingMessage(message);
        };

        websocket.onclose = function(event) {
            log(`WebSocket 연결 종료: ${event.code} ${event.reason}`, 'system');
            isConnected = false;
            isJoined = false;
            updateConnectionStatus();
            resetChatRoom();
        };

        websocket.onerror = function(error) {
            log(`WebSocket 오류: ${error}`, 'error');
        };

    } catch (error) {
        log(`연결 실패: ${error}`, 'error');
    }
}

// 상대방 역할 업데이트
function updatePartnerRole() {
    const userRole = document.getElementById('userRole').value;
    const partnerRoleDisplay = document.getElementById('partnerRoleDisplay');

    // 내 역할과 반대로 상대방 역할 설정
    const partnerRole = userRole === 'INFLUENCER' ? 'ADVERTISER' : 'INFLUENCER';
    partnerRoleDisplay.textContent = partnerRole;
}
// JWT 토큰 인증
async function authenticate() {
    const userId = document.getElementById('userId').value;
    const userRole = document.getElementById('userRole').value;
    const userEmail = document.getElementById('userEmail').value;

    if (!userId || !userEmail) {
        showAuthStatus('사용자 ID와 이메일을 입력해주세요.', false);
        return;
    }

    elements.authenticateBtn.disabled = true;
    showAuthStatus('JWT 토큰을 생성하는 중...', true);

    try {
        const response = await fetch('http://localhost:8080/api/test/jwt/generate', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: new URLSearchParams({
                userId: userId,
                role: userRole,
                email: userEmail
            })
        });

        const data = await response.json();

        if (data.status === 'SUCCESS') {
            jwtToken = data.token;
            showAuthStatus(`인증 성공! 토큰이 생성되었습니다.`, true);
            log(`JWT 토큰 생성 성공: ${userId} (${userRole})`, 'system');

            // 연결 버튼 활성화
            elements.connectBtn.disabled = false;
        } else {
            showAuthStatus(`인증 실패: ${data.message}`, false);
            log(`JWT 토큰 생성 실패: ${data.message}`, 'error');
        }
    } catch (error) {
        showAuthStatus('인증 중 오류가 발생했습니다.', false);
        log(`JWT 토큰 생성 오류: ${error}`, 'error');
    } finally {
        elements.authenticateBtn.disabled = false;
    }
}

// 상대방과 연결
async function connectToPartner() {
    if (!jwtToken) {
        showConnectStatus('먼저 인증을 완료해주세요.', false);
        return;
    }

    const partnerId = parseInt(document.getElementById('partnerId').value);
    const userId = parseInt(document.getElementById('userId').value);
    const userRole = document.getElementById('userRole').value;

    if (!partnerId || !userId) {
        showConnectStatus('상대방 ID와 사용자 ID를 입력해주세요.', false);
        return;
    }

    elements.connectBtn.disabled = true;
    showConnectStatus('채팅방을 생성하는 중...', true);

    try {
        // 1. 채팅방 생성 API 호출
        const chatRoomResponse = await createChatRoomAPI(userId, partnerId, userRole);
        if (!chatRoomResponse.success) {
            showConnectStatus(`채팅방 생성에 실패했습니다: ${chatRoomResponse.error}`, false);
            elements.connectBtn.disabled = false;
            return;
        }

        currentChatRoomId = chatRoomResponse.chatRoomId;
        currentPartnerId = partnerId;

        log(`채팅방 생성 성공: ${currentChatRoomId}`, 'system');
        showChatRoom(currentChatRoomId);

        // 2. WebSocket 연결
        if (!isConnected) {
            connect();
            await new Promise((resolve, reject) => {
                let attempts = 0;
                const maxAttempts = 10;

                const checkConnection = () => {
                    attempts++;
                    if (isConnected) {
                        resolve();
                    } else if (attempts >= maxAttempts) {
                        reject(new Error('WebSocket 연결 시간 초과'));
                    } else {
                        setTimeout(checkConnection, 100);
                    }
                };
                checkConnection();
            });
        }

        log('WebSocket 연결 완료', 'system');

        // 3. 채팅방 참여
        await connectChatRoom();

        // 4. 기존 메시지 로드
        await loadChatMessages();

    } catch (error) {
        log(`연결 실패: ${error}`, 'error');
        showConnectStatus('연결 중 오류가 발생했습니다.', false);
        elements.connectBtn.disabled = false;
    }
}

// 채팅방 생성 API 호출
async function createChatRoomAPI(userId, partnerId, userRole) {
    try {
        // 올바른 API 엔드포인트와 쿼리 파라미터 방식
        const influencerId = userRole === 'INFLUENCER' ? userId : partnerId;
        const advertiserId = userRole === 'INFLUENCER' ? partnerId : userId;

        const url = `http://localhost:8080/api/v1/chatRoom?influencer=${influencerId}&advertiser=${advertiserId}`;

        log(`채팅방 생성 요청: ${url}`, 'system');

        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${jwtToken}`,
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        const data = await response.json();
        log(`채팅방 생성 응답: ${JSON.stringify(data)}`, 'system');

        if (data.status === 'SUCCESS') {
            return {
                success: true,
                chatRoomId: data.data.chatRoomId
            };
        } else {
            return {
                success: false,
                error: data.data?.errorCode || 'UNKNOWN_ERROR'
            };
        }
    } catch (error) {
        log(`채팅방 생성 API 오류: ${error}`, 'error');
        return {
            success: false,
            error: error.message
        };
    }
}

// 채팅방 참여 (WebSocket)
async function connectChatRoom() {
    const userId = parseInt(document.getElementById('userId').value);
    const userRole = document.getElementById('userRole').value;

    const connectMessage = {
        type: 'CONNECT',
        chatRoomId: currentChatRoomId,
        senderId: userId,
        userRole: userRole
    };

    sendWebSocketMessage(connectMessage);
}

// 채팅 메시지 로드
async function loadChatMessages(before = null) {
    if (isLoadingMessages) {
        log(`로딩 중복 방지: 이미 로딩 중 (before: ${before})`, 'system');
        return; // 이미 로딩 중이면 중복 요청 방지
    }

    try {
        isLoadingMessages = true;

        let url = `http://localhost:8080/api/v1/chatRoom/${currentChatRoomId}/message?size=150`; // size 미포함 시 default=150
        if (before) {
            url += `&before=${before}`;
        }

        log(`메시지 로드 요청: ${url}, oldestMessageId: ${oldestMessageId}`, 'system');
        const response = await fetch(url, {
            headers: {
                'Authorization': `Bearer ${jwtToken}`,
                'Content-Type': 'application/json'
            }
        });
        const data = await response.json();

        if (data.status === 'SUCCESS') {
            const messages = data.data.messages;
            log(`메시지 ${messages.length}개 로드됨, hasMore: ${data.data.hasMore}`, 'system');

            if (messages.length > 0) {
                if (before) {
                    // 이전 메시지 로드 시 - 맨 위에 추가 (스크롤 위치 유지)
                    const scrollHeight = elements.messages.scrollHeight;
                    const scrollTop = elements.messages.scrollTop;

                    log(`스크롤 조정 전: scrollHeight=${scrollHeight}, scrollTop=${scrollTop}`, 'system');

                    // API에서 최신순(DESC)으로 온 메시지를 시간순으로 맨 위에 추가
                    // 가장 최신 것부터 맨 위에 추가해야, 최종적으로 오래된 것이 맨 위에 오게 됨
                    for (let i = 0; i < messages.length; i++) {
                        addMessageFromHistory(messages[i], false); // 맨 위에 추가 (최신것부터)
                    }

                    // 스크롤 위치 조정 (기존 읽던 위치 유지)
                    const newScrollHeight = elements.messages.scrollHeight;
                    const heightDifference = newScrollHeight - scrollHeight;
                    const newScrollTop = scrollTop + heightDifference;

                    // DOM 업데이트 후 스크롤 위치 설정 (비동기 처리)
                    setTimeout(() => {
                        const adjustedScrollTop = Math.max(newScrollTop, 100);
                        elements.messages.scrollTop = adjustedScrollTop;
                        log(`스크롤 조정 완료: scrollHeight=${elements.messages.scrollHeight}, scrollTop=${elements.messages.scrollTop}, heightDiff=${heightDifference}`, 'system');
                    }, 10);

                    log(`스크롤 조정 시작: scrollHeight=${newScrollHeight}, 계산된 scrollTop=${newScrollTop}, heightDiff=${heightDifference}`, 'system');

                    // 가장 오래된 메시지 ID 업데이트 (배열의 마지막이 가장 오래된 것)
                    const prevOldest = oldestMessageId;
                    oldestMessageId = messages[messages.length - 1].messageId;
                    log(`oldestMessageId 업데이트: ${prevOldest} -> ${oldestMessageId}`, 'system');
                } else {
                    // 최초 로드 시 - API에서 최신순(DESC)으로 온 것을 시간순으로 표시
                    // 가장 오래된 메시지 ID를 먼저 저장 (reverse 전 배열의 마지막)
                    oldestMessageId = messages[messages.length - 1].messageId;
                    log(`최초 로드 - oldestMessageId 설정: ${oldestMessageId}`, 'system');

                    // messages 배열을 뒤집어서 오래된 것부터 표시
                    messages.reverse().forEach(msg => {
                        addMessageFromHistory(msg, true); // 맨 아래 추가
                    });
                    elements.messages.scrollTop = elements.messages.scrollHeight;
                }

                // 더 불러올 메시지가 있는지 확인 (자동 로딩만 사용)
                hasMoreMessages = data.data.hasMore;
            }
        }
    } catch (error) {
        log(`메시지 로드 실패: ${error}`, 'error');
    } finally {
        isLoadingMessages = false;
    }
}

// 더 많은 메시지 로드
function loadMoreMessages() {
    if (oldestMessageId) {
        log(`loadMoreMessages 호출: oldestMessageId=${oldestMessageId}`, 'system');
        loadChatMessages(oldestMessageId);
    } else {
        log(`loadMoreMessages 호출했지만 oldestMessageId가 없음`, 'system');
    }
}

// 채팅방 나가기 (REST API)
async function leaveChatRoom() {
    if (!confirm('정말로 채팅방을 나가시겠습니까?')) {
        return;
    }

    try {
        const userId = parseInt(document.getElementById('userId').value);
        const userRole = document.getElementById('userRole').value;

        // 올바른 API 엔드포인트 (Authorization 헤더 필요)
        const url = `http://localhost:8080/api/v1/chatRoom/${currentChatRoomId}`;
        const response = await fetch(url, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${jwtToken}`,
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        const data = await response.json();
        log(`채팅방 나가기 응답: ${JSON.stringify(data)}`, 'system');

        if (data.status === 'SUCCESS') {
            log(`채팅방 나가기 성공`, 'system');

            // REST API에서 이미 DB 저장 + WebSocket 브로드캐스트 완료됨
            // 채팅방 초기화
            resetChatRoom();
        } else {
            alert(`채팅방 나가기에 실패했습니다: ${data.data?.errorCode || 'UNKNOWN_ERROR'}`);
        }
    } catch (error) {
        log(`채팅방 나가기 실패: ${error}`, 'error');
        alert('채팅방 나가기 중 오류가 발생했습니다.');
    }
}

// 텍스트 메시지 전송
async function sendMessage() {
    const messageText = elements.messageInput.value.trim();
    if (!messageText) {
        alert('메시지를 입력해주세요.');
        return;
    }

    try {
        const requestData = {
            senderId: parseInt(document.getElementById('userId').value),
            content: messageText,
            userRole: document.getElementById('userRole').value,
            messageType: document.getElementById('messageType').value
        };

        const response = await fetch(`http://localhost:8080/api/v1/chatRoom/${currentChatRoomId}/message`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${jwtToken}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestData)
        });

        if (!response.ok) {
            alert('메시지 전송에 실패했습니다.');
            return;
        }

        const result = await response.json();
        if (result.status !== 'SUCCESS') {
            alert('메시지 전송에 실패했습니다.');
            return;
        }

        log(`텍스트 메시지 전송 성공`, 'system');

        // REST API에서 이미 DB 저장 + WebSocket 브로드캐스트 완료됨
        elements.messageInput.value = '';

    } catch (error) {
        log(`메시지 전송 실패: ${error}`, 'error');
        alert('메시지 전송 중 오류가 발생했습니다.');
    }
}

// 파일 메시지 전송
async function sendFileMessage() {
    const fileInput = elements.fileInput;
    const messageType = document.getElementById('messageType').value;

    if (!fileInput.files[0]) {
        alert('파일을 선택해주세요.');
        return;
    }

    const file = fileInput.files[0];

    try {
        // 1. 서버에서 presigned URL 받아오기
        const formData = new FormData();
        formData.append('file', file);
        formData.append('senderId', document.getElementById('userId').value);
        formData.append('userRole', document.getElementById('userRole').value);
        formData.append('messageType', messageType);

        const response = await fetch(`http://localhost:8080/api/v1/chatRoom/${currentChatRoomId}/message`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${jwtToken}`
            },
            body: formData
        });

        const result = await response.json();
        if (result.status !== 'SUCCESS') {
            alert('파일 업로드 URL 생성에 실패했습니다.');
            return;
        }

        log(`파일 업로드 성공: ${file.name}`, 'system');

        // 입력 필드 초기화
        fileInput.value = '';

    } catch (error) {
        log(`파일 업로드 실패: ${error}`, 'error');
        alert('파일 업로드 중 오류가 발생했습니다.');
    }
}

// WebSocket 메시지 전송
function sendWebSocketMessage(message) {
    if (!isConnected) {
        alert('WebSocket이 연결되어 있지 않습니다.');
        return;
    }

    const messageStr = JSON.stringify(message);
    websocket.send(messageStr);
    log(`전송: ${messageStr}`, 'sent');
}

// WebSocket 수신 메시지 처리
function handleIncomingMessage(message) {
    switch (message.type) {
        case 'CONNECT_SUCCESS':
            isJoined = true;
            showConnectStatus(`채팅방 ${message.chatRoomId} 연결 완료`, true);
            showChatRoom(message.chatRoomId);
            break;

        case 'CHAT':
            const userLabel = getUserLabel(message.senderId, message.userRole);
            const isMyMessage = isMyMsg(message.senderId, message.userRole);
            addMessage(`${userLabel}: ${message.content}`, isMyMessage ? 'sent' : 'received');
            break;

        case 'FILE':
            const fileUserLabel = getUserLabel(message.senderId, message.userRole);
            const isMyFileMessage = isMyMsg(message.senderId, message.userRole);
            if (message.messageType === 'IMAGE') {
                addImageMessage(fileUserLabel, message.fileName, message.fileUrl, message.fileSize, isMyFileMessage);
            } else {
                addFileMessage(fileUserLabel, message.fileName, message.fileUrl, message.fileSize, isMyFileMessage);
            }
            break;

        case 'CONNECT':
            isJoined = true;
            log(`${message.userRole}-${message.senderId}님이 채팅방 ${message.chatRoomId}에 연결되었습니다.`, 'system');
            break;

        case 'DISCONNECT':
            log(`${message.userRole}-${message.senderId}님이 채팅방 ${message.chatRoomId}에서 연결 종료했습니다.`, 'system');
            break;

        case 'LEAVE':
            log(`${message.userRole}-${message.senderId}님이 채팅방을 나갔습니다.`);
            addMessage(`${message.userRole}-${message.senderId}님이 채팅방을 나갔습니다.`,'system');
            break;

        case 'ERROR':
            addMessage(`오류: ${message.content}`, 'error');
            showConnectStatus(`오류: ${message.content}`, false);
            elements.connectBtn.disabled = false;
            break;

        default:
            addMessage(`알 수 없는 메시지: ${JSON.stringify(message)}`, 'system');
    }
}

// 사용자 라벨 생성
function getUserLabel(userId, userRole) {
    const currentUserId = parseInt(document.getElementById('userId').value);
    const currentUserRole = document.getElementById('userRole').value;

    if (userId === currentUserId && userRole === currentUserRole) {
        return '나';
    }
    return '상대';
}

// 내 메시지인지 확인
function isMyMsg(userId, userRole) {
    const currentUserId = parseInt(document.getElementById('userId').value);
    const currentUserRole = document.getElementById('userRole').value;

    return userId === currentUserId && userRole === currentUserRole;
}

// 파일 크기 포맷팅
function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

// 메시지 추가
function addMessage(text, type) {
    const messageElement = document.createElement('div');
    messageElement.className = `message ${type}`;
    messageElement.innerHTML = `<span>${getCurrentTime()}</span> ${text}`;
    elements.messages.appendChild(messageElement);
    elements.messages.scrollTop = elements.messages.scrollHeight;
}

// 이미지 메시지 추가
function addImageMessage(userLabel, fileName, fileUrl, fileSize, isMyMessage = false) {
    const messageElement = document.createElement('div');
    messageElement.className = `message ${isMyMessage ? 'sent' : 'received'}`;
    messageElement.innerHTML = `
        <span>${getCurrentTime()}</span> ${userLabel}: [이미지] ${fileName} (${formatFileSize(fileSize)})
        <br><img src="${fileUrl}" alt="${fileName}" onclick="window.open('${fileUrl}', '_blank')">
    `;
    elements.messages.appendChild(messageElement);
    elements.messages.scrollTop = elements.messages.scrollHeight;
}

// 파일 메시지 추가
function addFileMessage(userLabel, fileName, fileUrl, fileSize, isMyMessage = false) {
    const messageElement = document.createElement('div');
    messageElement.className = `message ${isMyMessage ? 'sent' : 'received'}`;
    messageElement.innerHTML = `
        <span>${getCurrentTime()}</span> ${userLabel}: [파일] ${fileName} (${formatFileSize(fileSize)})
        <br><a href="${fileUrl}" target="_blank">📎 ${fileName} 다운로드</a>
    `;
    elements.messages.appendChild(messageElement);
    elements.messages.scrollTop = elements.messages.scrollHeight;
}

// 연결 상태 표시
function showConnectStatus(message, isSuccess) {
    elements.connectStatus.style.display = 'block';
    elements.connectStatusText.textContent = message;
    elements.connectStatus.className = `connect-status ${isSuccess ? 'connect-success' : 'connect-error'}`;
}

// 인증 상태 표시
function showAuthStatus(message, isSuccess) {
    elements.authStatus.style.display = 'block';
    elements.authStatusText.textContent = message;
    elements.authStatus.className = `connect-status ${isSuccess ? 'connect-success' : 'connect-error'}`;
}

// 채팅방 표시
function showChatRoom(chatRoomId) {
    elements.chatContainer.style.display = 'block';
    elements.chatRoomTitle.textContent = `채팅방 ${chatRoomId} (상대: ${currentPartnerId})`;
    elements.connectBtn.style.display = 'none';
}

// 채팅방 초기화
function resetChatRoom() {
    isJoined = false;
    elements.chatContainer.style.display = 'none';
    elements.connectStatus.style.display = 'none';
    elements.connectBtn.style.display = 'inline-block';
    elements.connectBtn.disabled = false;
    currentChatRoomId = null;
    currentPartnerId = null;
    oldestMessageId = null;
    hasMoreMessages = true;
}

// 히스토리에서 메시지 추가
function addMessageFromHistory(msg, append = true) {
    const userLabel = getUserLabel(msg.senderId, msg.userRole);
    const isMyMessage = isMyMsg(msg.senderId, msg.userRole);
    const messageTime = new Date(msg.createdAt).toLocaleTimeString();

    const messageElement = document.createElement('div');
    messageElement.className = `message ${isMyMessage ? 'sent' : 'received'}`;

    if (msg.messageType === 'TEXT') {
        messageElement.innerHTML = `<span>${messageTime}</span> ${userLabel}: ${msg.content}`;
    } else if (msg.messageType === 'IMAGE') {
        messageElement.innerHTML = `
            <span>${messageTime}</span> ${userLabel}: [이미지] ${msg.fileName || 'image'} (${formatFileSize(msg.fileSize || 0)})
            <br><img src="${msg.content}" alt="${msg.fileName || 'image'}" onclick="window.open('${msg.content}', '_blank')">
        `;
    } else if (msg.messageType === 'FILE') {
        messageElement.innerHTML = `
            <span>${messageTime}</span> ${userLabel}: [파일] ${msg.fileName || 'file'} (${formatFileSize(msg.fileSize || 0)})
            <br><a href="${msg.content}" target="_blank">📎 ${msg.fileName || 'file'} 다운로드</a>
        `;
    } else if (msg.messageType === 'JOIN') {
        // 입장 메시지 - 시스템 메시지로 표시
        messageElement.className = 'message system';
        messageElement.innerHTML = `<span>${messageTime}</span> ${msg.userRole}-${msg.senderId}님이 채팅방에 입장했습니다.`;
    } else if (msg.messageType === 'LEAVE') {
        // 나가기 메시지 - 시스템 메시지로 표시
        messageElement.className = 'message system';
        messageElement.innerHTML = `<span>${messageTime}</span> ${msg.userRole}-${msg.senderId}님이 채팅방을 나갔습니다.`;
    }

    if (append) {
        elements.messages.appendChild(messageElement);
    } else {
        elements.messages.insertBefore(messageElement, elements.messages.firstChild);
    }
}

// 메시지 입력 방식 토글
function toggleMessageInput() {
    const messageType = document.getElementById('messageType').value;

    if (messageType === 'TEXT') {
        elements.textMessageDiv.style.display = 'flex';
        elements.fileMessageDiv.style.display = 'none';
    } else {
        elements.textMessageDiv.style.display = 'none';
        elements.fileMessageDiv.style.display = 'flex';
    }
}

// 로그 추가
function log(text, type) {
    const logElement = document.createElement('div');
    logElement.className = `message ${type}`;
    logElement.innerHTML = `<span>${getCurrentTime()}</span> ${text}`;
    elements.logs.appendChild(logElement);
    elements.logs.scrollTop = elements.logs.scrollHeight;
}

// 현재 시간 가져오기
function getCurrentTime() {
    return new Date().toLocaleTimeString();
}

// 로그 지우기
function clearLogs() {
    elements.logs.innerHTML = '';
}

// 연결 상태 업데이트
function updateConnectionStatus() {
    if (isConnected) {
        elements.connectionStatus.textContent = '연결됨';
        elements.connectionStatus.className = 'status connected';
    } else {
        elements.connectionStatus.textContent = '연결 안됨';
        elements.connectionStatus.className = 'status disconnected';
    }
}

// Enter 키 이벤트
document.addEventListener('keypress', function(e) {
    if (e.key === 'Enter') {
        if (document.getElementById('messageType').value === 'TEXT' && elements.messageInput === document.activeElement) {
            sendMessage();
        }
    }
});

// 스크롤 이벤트 - 맨 위로 스크롤하면 이전 메시지 자동 로드
function setupScrollListener() {
    let scrollTimeout = null;

    elements.messages.addEventListener('scroll', function() {
        // 디바운싱: 스크롤이 멈춘 후 100ms 후에 실행
        if (scrollTimeout) {
            clearTimeout(scrollTimeout);
        }

        scrollTimeout = setTimeout(() => {
            const scrollTop = elements.messages.scrollTop;
            const scrollHeight = elements.messages.scrollHeight;
            const clientHeight = elements.messages.clientHeight;

            // 위쪽에서 스크롤할 때 로드 (scrollTop이 800 이하 또는 상위 80% 이내)
            const triggerDistance = Math.min(800, clientHeight * 0.8);
            if (scrollTop <= triggerDistance && oldestMessageId && hasMoreMessages && !isLoadingMessages) {
                log(`스크롤 자동 로드 트리거: scrollTop=${scrollTop}, triggerDistance=${triggerDistance}, scrollHeight=${scrollHeight}, clientHeight=${clientHeight}`, 'system');
                loadMoreMessages();
            } else if (scrollTop <= triggerDistance) {
                log(`스크롤 자동 로드 조건 불충족: isLoadingMessages=${isLoadingMessages}, hasLoadMoreBtn=${elements.loadMoreBtn.style.display === 'block'}, hasOldestId=${!!oldestMessageId}`, 'system');
            }
        }, 100);
    });
}

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', function() {
    log('채팅 테스트 페이지가 로드되었습니다.', 'system');
    updateConnectionStatus();
    updatePartnerRole(); // 초기 상대방 역할 설정
    toggleMessageInput(); // 초기 메시지 입력 방식 설정
    setupScrollListener(); // 스크롤 이벤트 리스너 설정

    // 연결 버튼 초기 비활성화 (인증 후 활성화)
    elements.connectBtn.disabled = true;
});